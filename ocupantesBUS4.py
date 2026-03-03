import paho.mqtt.client as mqtt
import argparse
import sys
import time
import json
from datetime import datetime, timedelta, timezone

# -------------------------------
# Argumentos
# -------------------------------
parser = argparse.ArgumentParser(
    description="Lista ocupación de buses"
)

parser.add_argument("--src-broker", required=True, help="Broker origen")
parser.add_argument("--src-port", type=int, default=1883)
parser.add_argument("--src-user", help="Usuario broker origen")
parser.add_argument("--src-pass", help="Password broker origen")


args = parser.parse_args()

# -------------------------------
# Estructura para almacenar ocupación
# -------------------------------
# clave: "empresa:bus_nro", valor: {"ocupantes": int, "timestamp": float}
ocupacion_buses = {}

# Tiempo límite en días
TIEMPO_LIMITE = timedelta(days=6)

# -------------------------------
# Cliente origen (subscriber)
# -------------------------------
src_client = mqtt.Client(client_id="replicador-src")

if args.src_user:
    src_client.username_pw_set(args.src_user, args.src_pass)

def on_connect_src(client, userdata, flags, rc):
    print(f"[SRC] Conectado (rc={rc})")
    client.subscribe("STMBuses/+/+/+/Vehiculo/Ocupacion/#", qos=0)
    print(f"[SRC] Suscripto a Ocupación")

def on_message_src(client, userdata, msg):
    try:
        # --- Monitoreo de ocupación ---
        topic_parts = msg.topic.split('/')
        # Patrón esperado: STMBuses/<empresa>/<bus>/<id>/Vehiculo/Ocupacion/<algo>
        if len(topic_parts) >= 7 and topic_parts[0] == "STMBuses" and topic_parts[4] == "Vehiculo" and topic_parts[5] == "Ocupacion":
            empresa = topic_parts[1]
            bus_nro = topic_parts[2]

            try:
                data = json.loads(msg.payload.decode('utf-8'))
                if "OcupantesBus" in data and "MDTEntidad" in data:
                    ocupantes = data["OcupantesBus"]
                    mdt_entidad = data["MDTEntidad"]

                    # Parsear MDTEntidad a datetime (offset-aware)
                    try:
                        timestamp = datetime.fromisoformat(mdt_entidad)
                    except ValueError:
                        print(f"*** ERROR: Formato de fecha incorrecto en MDTEntidad: {mdt_entidad}")
                        return

                    # Convertir datetime.now() a offset-aware con UTC
                    now = datetime.now(timezone.utc)

                    # Filtrar mensajes antiguos
                    if now - timestamp <= TIEMPO_LIMITE:
                        print(f"*** OCUPACIÓN: Bus [Empresa:{empresa} Nro:{bus_nro}] -> OcupantesBus: {ocupantes}")
                        
                        # Guardar en la estructura
                        clave = f"{empresa}:{bus_nro}"
                        ocupacion_buses[clave] = {
                            "ocupantes": ocupantes,
                            "timestamp": timestamp.timestamp()  # Guardar como timestamp flotante
                        }
                    else:
                        print(f"*** AVISO: Mensaje descartado por ser antiguo (Bus Nro: {bus_nro})")
                else:
                    print(f"*** AVISO: mensaje de ocupación sin campos requeridos en {msg.topic}")
            except json.JSONDecodeError:
                print(f"*** ERROR: no se pudo decodificar JSON en {msg.topic}")
            except Exception as e:
                print(f"*** ERROR procesando ocupación: {e}")

    except Exception as e:
        print(f"Error replicando mensaje: {e}")

src_client.on_connect = on_connect_src
src_client.on_message = on_message_src

# -------------------------------
# Función para volcar resumen
# -------------------------------
def volcar_resumen():
    print("\n" + "="*50)
    print("RESUMEN DE OCUPACIÓN DE BUSES (últimos valores):")
    if not ocupacion_buses:
        print("No se recibió ningún dato de ocupación.")
    else:
        # Ordenar por clave (empresa, bus_nro)
        items_ordenados = sorted(ocupacion_buses.items(), key=lambda x: x[0])
        for clave, info in items_ordenados:
            empresa, bus_nro = clave.split(':')
            ocupantes = info["ocupantes"]
            hora = time.strftime('%Y-%m-%d %H:%M:%S', time.localtime(info["timestamp"]))
            print(f"  Empresa: {empresa}, Nro: {bus_nro} -> Ocupantes: {ocupantes} (última actualización: {hora})")
    print("="*50)

# -------------------------------
# Main
# -------------------------------
try:
    src_client.connect(args.src_broker, args.src_port, 60)
    src_client.loop_forever()

except KeyboardInterrupt:
    print("\nDeteniendo replicador...")
    volcar_resumen()

finally:
    src_client.disconnect()
    print("Conexiones cerradas")
