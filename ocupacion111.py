import json
import time
from datetime import datetime, timezone
import paho.mqtt.client as mqtt
from openpyxl import Workbook
import uuid

# -------------------------------
# Configuración embebida del broker
# -------------------------------
BROKER = "broker-stm-preprod.apps.ocp4-prod.imm.gub.uy"
PORT = 1883
USERNAME = "stm"
PASSWORD = "Y4208BOly7ZM"
CLIENT_ID = f"buses-50-111-{uuid.uuid4().hex}"
src_client = mqtt.Client(client_id=CLIENT_ID)

# Suscripciones específicas para empresa 50 y bus 1049
TOPICS = [
    "STMBuses/50/111/+/Vehiculo/Ocupacion/3_0",
    "STMBuses/50/111/+/DatosOperacionales/EstadoOperativo/3_0",
    "STMBuses/50/111/+/DatosOperacionales/Parada/3_0",
]

# Archivo de log (Excel)
LOG_FILENAME = f"buses_log_{datetime.now().strftime('%Y%m%d_%H%M%S')}.xlsx"

# Crear workbook y hoja
wb = Workbook()
ws = wb.active
ws.title = "Mensajes"
ws.append([
    "Timestamp_UTC", "Empresa", "Bus_Nro", "Bus_ID",
    "Categoria", "Subcategoria", "Algo", "Payload_Preview"
])

messages_logged = 0

# -------------------------------
# Cliente MQTT (subscriber)
# -------------------------------
src_client = mqtt.Client(client_id="buses-reporte-diario_embebido")

if USERNAME:
    src_client.username_pw_set(USERNAME, PASSWORD)

def on_connect_src(client, userdata, flags, rc):
    print(f"[SRC] Conectado (rc={rc})")
    for t in TOPICS:
        client.subscribe(t, qos=0)
    print(f"[SRC] Suscrito a topics: {TOPICS}")

def on_message_src(client, userdata, msg):
    global messages_logged
    try:
        topic_parts = msg.topic.split('/')
        # Patrón esperado: STMBuses/<empresa>/<bus>/<id>/<categoria>/<subcategoria>/<algo>
        if len(topic_parts) >= 7 and topic_parts[0] == "STMBuses" and topic_parts[1] == "50" and topic_parts[2] == "111":
            empresa = topic_parts[1]
            bus_nro = topic_parts[2]
            bus_id = topic_parts[3]
            categoria = topic_parts[4]
            subcategoria = topic_parts[5]
            algo = topic_parts[6]

            # Intentar decodificar payload como JSON, si falla usar texto
            payload = ""
            preview = ""
            try:
                data = json.loads(msg.payload.decode('utf-8'))
                payload = json.dumps(data, ensure_ascii=False)
            except Exception:
                payload = msg.payload.decode('utf-8', errors='ignore')

            preview = payload[:300] if payload else ""

            # Timestamp en UTC
            timestamp_utc = datetime.now(timezone.utc).strftime('%Y-%m-%d %H:%M:%S %Z')

            # Escribir en Excel
            ws.append([timestamp_utc, empresa, bus_nro, bus_id, categoria, subcategoria, algo, preview])
            wb.save(LOG_FILENAME)
            messages_logged += 1
            print(f"LOG: {empresa}:{bus_nro}:{bus_id} {categoria}/{subcategoria}/{algo} @ {timestamp_utc}")
    except Exception as e:
        print(f"*** ERROR procesando mensaje: {e}")

# -------------------------------
# Main
# -------------------------------
src_client.on_connect = on_connect_src
src_client.on_message = on_message_src

try:
    src_client.connect(BROKER, PORT, 60)
    src_client.loop_forever()
except KeyboardInterrupt:
    print("\nDeteniendo monitoreo...")
    # Guardar cualquier dato pendiente (ya se guarda tras cada mensaje)
    wb.save(LOG_FILENAME)
    print(f"Archivo guardado: {LOG_FILENAME}")
finally:
    src_client.disconnect()
    print("Conexiones cerradas")
