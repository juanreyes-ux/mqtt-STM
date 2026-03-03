import paho.mqtt.client as mqtt
import argparse
import time
import json
from datetime import datetime, timedelta, timezone
from openpyxl import Workbook

# -------------------------------
# Argumentos
# -------------------------------
parser = argparse.ArgumentParser(
    description="Lista de buses reportados en la última semana"
)

parser.add_argument("--src-broker", required=True, help="Broker origen")
parser.add_argument("--src-port", type=int, default=1883)
parser.add_argument("--src-user", help="Usuario broker origen")
parser.add_argument("--src-pass", help="Password broker origen")


args = parser.parse_args()

# -------------------------------
# Estructura para almacenar buses reportados
# -------------------------------
# clave: "empresa:bus_nro", valor: timestamp de la última vez que reportaron
buses_reportados = {}

# Tiempo límite (últimos 6 días)
TIEMPO_LIMITE = timedelta(days=1)

# -------------------------------
# Cliente origen (subscriber)
# -------------------------------
src_client = mqtt.Client(client_id="buses-reporte-semanal")

if args.src_user:
    src_client.username_pw_set(args.src_user, args.src_pass)

def on_connect_src(client, userdata, flags, rc):
    print(f"[SRC] Conectado (rc={rc})")
    client.subscribe("STMBuses/20/+/+/Vehiculo/EmpresaBus/3_0", qos=0)
    print(f"[SRC] Suscripto a EmpresaBus")

def on_message_src(client, userdata, msg):
    try:
        # --- Monitoreo de buses reportados ---
        topic_parts = msg.topic.split('/')
        # Patrón esperado: STMBuses/50/<bus>/<id>/Vehiculo/EmpresaBus/3_0
        if len(topic_parts) >= 7 and topic_parts[0] == "STMBuses" and topic_parts[1] == "20":
            bus_nro = topic_parts[2]
            bus_id = topic_parts[3]

            try:
                # Parsear el payload del mensaje
                data = json.loads(msg.payload.decode('utf-8'))
                if "MDTEntidad" in data:
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
                        clave = f"20:{bus_nro}:{bus_id}"
                        buses_reportados[clave] = timestamp.timestamp()  # Guardar como timestamp flotante
                        print(f"*** REPORTE: Bus [Empresa:20 Nro:{bus_nro} ID:{bus_id}] reportó EmpresaBus")
                    else:
                        print(f"*** AVISO: Mensaje descartado por ser antiguo (Bus Nro: {bus_nro})")
                else:
                    print(f"*** AVISO: mensaje de EmpresaBus sin campo MDTEntidad en {msg.topic}")

            except json.JSONDecodeError:
                print(f"*** ERROR: No se pudo decodificar JSON en {msg.topic}")
            except Exception as e:
                print(f"*** ERROR procesando mensaje: {e}")

    except Exception as e:
        print(f"Error replicando mensaje: {e}")

src_client.on_connect = on_connect_src
src_client.on_message = on_message_src

# -------------------------------
# Función para volcar resumen y guardar en Excel
# -------------------------------
def volcar_resumen_a_excel():
    print("\n" + "="*50)
    print("RESUMEN DE BUSES QUE REPORTARON EN LA ÚLTIMA SEMANA:")
    if not buses_reportados:
        print("No se recibió ningún reporte de buses.")
    else:
        # Crear un archivo Excel
        wb = Workbook()
        ws = wb.active
        ws.title = "Buses Reportados"

        # Agregar encabezados
        ws.append(["Empresa", "Número de Bus", "ID de Bus", "Último Reporte"])

        # Ordenar por clave (empresa, bus_nro, bus_id)
        items_ordenados = sorted(buses_reportados.items(), key=lambda x: x[0])
        for clave, timestamp in items_ordenados:
            empresa, bus_nro, bus_id = clave.split(':')
            hora = time.strftime('%Y-%m-%d %H:%M:%S', time.localtime(timestamp))
            ws.append([empresa, bus_nro, bus_id, hora])
            print(f"  Empresa: {empresa}, Nro: {bus_nro}, ID: {bus_id} -> Último reporte: {hora}")
        
        # Guardar el archivo Excel con un nombre único
        nombre_archivo = f"buses_reportados_{datetime.now().strftime('%Y%m%d_%H%M%S')}.xlsx"
        wb.save(nombre_archivo)
        print(f"\nResumen guardado en el archivo Excel: {nombre_archivo}")

    print("="*50)

# -------------------------------
# Main
# -------------------------------
try:
    src_client.connect(args.src_broker, args.src_port, 60)
    src_client.loop_forever()

except KeyboardInterrupt:
    print("\nDeteniendo monitoreo...")
    volcar_resumen_a_excel()

finally:
    src_client.disconnect()
    print("Conexiones cerradas")
