import paho.mqtt.client as mqtt
import argparse
import sys
import time
import json

# -------------------------------
# Argumentos
# -------------------------------
parser = argparse.ArgumentParser(
    description="Replica tópicos MQTT de un broker a otro y lista ocupación de buses"
)

parser.add_argument("--src-broker", required=True, help="Broker origen")
parser.add_argument("--src-port", type=int, default=1883)
parser.add_argument("--src-user", help="Usuario broker origen")
parser.add_argument("--src-pass", help="Password broker origen")

parser.add_argument("--dst-broker", required=True, help="Broker destino")
parser.add_argument("--dst-port", type=int, default=1883)
parser.add_argument("--dst-user", help="Usuario broker destino")
parser.add_argument("--dst-pass", help="Password broker destino")

parser.add_argument("--topic", default="STMBuses/#", help="Tópico a replicar (por defecto todos los STMBuses)")

args = parser.parse_args()

# -------------------------------
# Estructura para almacenar ocupación
# -------------------------------
# clave: "empresa:bus_nro:bus_id", valor: {"ocupantes": int, "timestamp": float}
ocupacion_buses = {}

# -------------------------------
# Cliente destino (publisher)
# -------------------------------
dst_client = mqtt.Client(client_id="replicador-dst")

if args.dst_user:
    dst_client.username_pw_set(args.dst_user, args.dst_pass)

def on_connect_dst(client, userdata, flags, rc):
    print(f"[DST] Conectado (rc={rc})")

dst_client.on_connect = on_connect_dst
dst_client.connect(args.dst_broker, args.dst_port, 60)
dst_client.loop_start()

# -------------------------------
# Cliente origen (subscriber)
# -------------------------------
src_client = mqtt.Client(client_id="replicador-src")

if args.src_user:
    src_client.username_pw_set(args.src_user, args.src_pass)

def on_connect_src(client, userdata, flags, rc):
    print(f"[SRC] Conectado (rc={rc})")
    client.subscribe(args.topic, qos=0)
    print(f"[SRC] Suscripto a {args.topic}")

def on_message_src(client, userdata, msg):
    try:
        # Re-publicar tal cual (funcionalidad original)
        dst_client.publish(
            topic=msg.topic,
            payload=msg.payload,
            qos=msg.qos,
            retain=msg.retain
        )
        #print(f"→ {msg.topic} (qos={msg.qos}, retain={msg.retain})")

        # --- Monitoreo de ocupación ---
        topic_parts = msg.topic.split('/')
        # Patrón esperado: STMBuses/<empresa>/<bus>/<id>/Vehiculo/Ocupacion/<algo>
        if len(topic_parts) >= 7 and topic_parts[0] == "STMBuses" and topic_parts[4] == "Vehiculo" and topic_parts[5] == "Ocupacion":
            empresa = topic_parts[1]
            bus_nro = topic_parts[2]
            bus_id = topic_parts[3]

            try:
                data = json.loads(msg.payload.decode('utf-8'))
                if "OcupantesBus" in data:
                    ocupantes = data["OcupantesBus"]
                    print(f"*** OCUPACIÓN: Bus [Empresa:{empresa} Nro:{bus_nro} ID:{bus_id}] -> OcupantesBus: {ocupantes}")
                    
                    # Guardar en la estructura
                    clave = f"{empresa}:{bus_nro}:{bus_id}"
                    ocupacion_buses[clave] = {
                        "ocupantes": ocupantes,
                        "timestamp": time.time()
                    }
                else:
                    print(f"*** AVISO: mensaje de ocupación sin campo 'OcupantesBus' en {msg.topic}")
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
        # Ordenar por clave (empresa, bus_nro, bus_id)
        items_ordenados = sorted(ocupacion_buses.items(), key=lambda x: x[0])
        for clave, info in items_ordenados:
            empresa, bus_nro, bus_id = clave.split(':')
            ocupantes = info["ocupantes"]
            hora = time.strftime('%Y-%m-%d %H:%M:%S', time.localtime(info["timestamp"]))
            print(f"  Empresa: {empresa}, Nro: {bus_nro}, ID: {bus_id} -> Ocupantes: {ocupantes} (última actualización: {hora})")
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
    dst_client.disconnect()
    dst_client.loop_stop()
    print("Conexiones cerradas")
