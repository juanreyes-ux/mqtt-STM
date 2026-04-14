#!/usr/bin/env python3
import json
import time
from datetime import datetime, timezone
import paho.mqtt.client as mqtt

# Configuración del broker (embebida)

PORT = 1883

BROKER = "10.8.240.48"
USERNAME = "STM"
PASSWORD = "Jar11y"

#BROKER = "broker-stm-prod.apps.ocp4-prod.imm.gub.uy"
#USERNAME = "stm-admin"
#PASSWORD = "1Dw4KjIM1lQ"

# Suscripciones para empresa 50, bus 111
TOPICS = [
    "STMBuses/50/+/+/Vehiculo/Ocupacion/3_0",
    "STMBuses/50/+/+/DatosOperacionales/EstadoOperativo/3_0",
    "STMBuses/50/+/+/DatosOperacionales/Parada/3_0",
    "InfoLocal/Conteo/AscDescxParadaOcup",
]

CLIENT_ID = "bus4-JR"

LOG_FILE = "mensajes_bus4.txt"

def on_connect(client, userdata, flags, rc):
    print(f"[CONNECT] rc={rc}")
    for t in TOPICS:
        client.subscribe(t)
        print(f"[SUBSCRIBED] {t}")

def on_message(client, userdata, msg):
    # Hora en UTC
    ts = datetime.now(timezone.utc).strftime('%Y-%m-%d %H:%M:%S %Z')
    raw = msg.payload.decode('utf-8', errors='replace')

    # Intentar formatear como JSON bonito, si no es JSON usar texto plano
    try:
        data = json.loads(raw)
        payload_formatted = json.dumps(data, indent=2, ensure_ascii=False)
    except Exception:
        payload_formatted = raw

    # Mensaje formateado para consola
    console_block = (
        f"[{ts}] Topic: {msg.topic} | QoS={msg.qos} | Retain={msg.retain}\n"
        f"Payload:\n{payload_formatted}\n"
        + "-"*60
        
    )
    print(console_block)

    # Guardar en texto
    with open(LOG_FILE, "a", encoding="utf-8") as f:
        f.write(console_block + "\n")

client = mqtt.Client(client_id="bus111-logger_ordered")
client.username_pw_set(USERNAME, PASSWORD)
client.on_connect = on_connect
client.on_message = on_message

if __name__ == "__main__":
    try:
        client.connect(BROKER, PORT, 60)
        client.loop_forever()
    except KeyboardInterrupt:
        print("Deteniendo...")
    finally:
        client.disconnect()