import paho.mqtt.client as mqtt
import argparse
import json
from datetime import datetime, timedelta, timezone

# -------------------------------
# Argumentos
# -------------------------------
parser = argparse.ArgumentParser(
    description="Valida mensajes del tópico EstadoOperativo con Version=303 y dentro de los últimos 2 días"
)

parser.add_argument("--src-broker", required=True, help="Broker origen")
parser.add_argument("--src-port", type=int, default=1883)
parser.add_argument("--src-user", help="Usuario broker origen")
parser.add_argument("--src-pass", help="Password broker origen")
args = parser.parse_args()

# -------------------------------
# Variables globales
# -------------------------------
buses_validos = set()  # Almacena los IDs de los buses válidos
TIEMPO_LIMITE = timedelta(days=2)  # Solo considerar mensajes de los últimos 2 días

# -------------------------------
# Cliente MQTT
# -------------------------------
client = mqtt.Client(client_id="validador-estado-operativo")

if args.src_user:
    client.username_pw_set(args.src_user, args.src_pass)

def on_connect(client, userdata, flags, rc):
    print(f"[SRC] Conectado (rc={rc})")
    # Suscribirse solo al tópico relevante
    client.subscribe("STMBuses/+/+/+/DatosOperacionales/EstadoOperativo/3_0", qos=0)
    print(f"[SRC] Suscripto al tópico: STMBuses/+/+/+/DatosOperacionales/EstadoOperativo/3_0")

def on_message(client, userdata, msg):
    try:
        # Parsear el payload del mensaje
        data = json.loads(msg.payload.decode('utf-8'))

        # Validar que el campo "Version" existe y es igual a 303
        if "Version" in data and data["Version"] == 303:
            # Validar el campo MDTEntidad (timestamp)
            if "MDTEntidad" in data:
                try:
                    timestamp = datetime.fromisoformat(data["MDTEntidad"])  # Parsear MDTEntidad
                    now = datetime.now(timezone.utc)  # Hora actual en UTC

                    # Validar si el timestamp está dentro de los últimos 2 días
                    if now - timestamp <= TIEMPO_LIMITE:
                        empresa = msg.topic.split('/')[1]
                        bus_nro = msg.topic.split('/')[2]
                        id_hardware = msg.topic.split('/')[3]
                        bus_id = f"{empresa}:{bus_nro}:{id_hardware}"  # ID único del bus
                        buses_validos.add(bus_id)  # Agregar a la lista de buses válidos
                        print(f"✔️ [VALIDADO] Empresa: {empresa}, Bus Nro: {bus_nro}, ID Hardware: {id_hardware}")
                        print(f"    Payload: {json.dumps(data, indent=2)}")
                    else:
                        print(f"⚠️ [DESCARTADO] Tópico: {msg.topic}, MDTEntidad fuera del rango de 2 días (MDTEntidad: {data['MDTEntidad']})")
                except ValueError:
                    print(f"⚠️ [ERROR] Formato de MDTEntidad inválido: {data.get('MDTEntidad')}")
            else:
                print(f"⚠️ [DESCARTADO] Tópico: {msg.topic}, No contiene MDTEntidad")
        else:
            print(f"⚠️ [DESCARTADO] Tópico: {msg.topic}, Version no es 303 o no existe.")

    except json.JSONDecodeError:
        print(f"⚠️ [ERROR] No se pudo decodificar el mensaje en {msg.topic}")
    except Exception as e:
        print(f"⚠️ [ERROR] Procesando mensaje: {e}")

client.on_connect = on_connect
client.on_message = on_message

# -------------------------------
# Función para mostrar resumen al salir
# -------------------------------
def mostrar_resumen():
    print("\n" + "=" * 50)
    print("RESUMEN DE BUSES CON VERSION=303 Y TIMESTAMP VÁLIDO (ÚLTIMOS 2 DÍAS):")
    if buses_validos:
        for bus_id in sorted(buses_validos):
            print(f"  - {bus_id}")
    else:
        print("No se encontraron buses que cumplan las condiciones.")
    print("=" * 50)

# -------------------------------
# Main
# -------------------------------
try:
    client.connect(args.src_broker, args.src_port, 60)
    client.loop_forever()

except KeyboardInterrupt:
    print("\nDeteniendo monitoreo...")
    mostrar_resumen()

finally:
    client.disconnect()
    print("Conexión cerrada")
