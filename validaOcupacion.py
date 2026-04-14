import argparse
import json
import paho.mqtt.client as mqtt

# Configuración de argparse para recibir parámetros del broker
parser = argparse.ArgumentParser(description="Script para validar mensajes MQTT relacionados con ocupación de buses")
parser.add_argument("--src-broker", required=True, help="Broker origen")
parser.add_argument("--src-port", type=int, default=1883, help="Puerto del broker origen")
parser.add_argument("--src-user", help="Usuario broker origen")
parser.add_argument("--src-pass", help="Password broker origen")
parser.add_argument("--topic", required=True, help="Topic MQTT para escuchar mensajes")
args = parser.parse_args()

# Variables globales para validaciones
last_occupants = None  # Última ocupación total
last_parada = None  # Última parada procesada

# Callback para procesar mensajes MQTT
def on_message(client, userdata, msg):
    global last_occupants, last_parada

    # Print para mostrar el tópico y el mensaje recibido
    #print(f"Mensaje recibido en el topic '{msg.topic}': {msg.payload.decode('utf-8')}")

    try:
        # Parsear el mensaje recibido
        message = json.loads(msg.payload.decode("utf-8"))
        topic = msg.topic

        if "DatosOperacionales/AscDescParada" in topic:
            validate_asc_desc_parada(message)

        elif "Vehiculo/Ocupacion" in topic:
            validate_ocupacion(message)

    except json.JSONDecodeError:
        print(f"Error al decodificar el mensaje: {msg.payload}")


# Validación de mensajes AscDescParada
def validate_asc_desc_parada(message):
    global last_occupants, last_parada

    ascensos = sum([message.get(f"AscensoPuerta{i}", 0) for i in range(1, 5)])
    descensos = sum([message.get(f"DescensoPuerta{i}", 0) for i in range(1, 5)])
    parada = message.get("ParadaCodigo")

    print(f"Validando AscDescParada: Parada {parada}, Ascensos: {ascensos}, Descensos: {descensos}")

    # Actualizar última parada procesada
    last_parada = parada

# Validación de mensajes Vehiculo/Ocupacion
def validate_ocupacion(message):
    global last_occupants, last_parada

    total_ascensos = message.get("TotalAscensos", 0)
    total_descensos = message.get("TotalDescensos", 0)
    ocupantes = message.get("OcupantesBus", 0)
    parada = message.get("ParadaCodigo")

    print(f"Validando Vehiculo/Ocupacion: Parada {parada}, Total Ascensos: {total_ascensos}, Total Descensos: {total_descensos}, Ocupantes: {ocupantes}")

    # Validar consistencia de datos
    if last_occupants is not None:
        expected_occupants = last_occupants + total_ascensos - total_descensos
        if ocupantes != expected_occupants:
            print(f"ERROR: Inconsistencia en ocupación en Parada {parada}. Esperado: {expected_occupants}, Reportado: {ocupantes}")
        else:
            print("Ocupación validada correctamente.")

    # Actualizar última ocupación total
    last_occupants = ocupantes

# Configuración y conexión al broker MQTT
def main():
    # Crear cliente MQTT con Client ID personalizado
    client = mqtt.Client(client_id="juanReyes-preprod")

    # Configurar credenciales opcionales
    if args.src_user and args.src_pass:
        client.username_pw_set(args.src_user, args.src_pass)

    client.on_message = on_message

    # Conexión al broker
    try:
        client.connect(args.src_broker, args.src_port)
        client.subscribe(args.topic)

        print(f"Conectado al broker {args.src_broker} con Client ID 'juanReyes-preprod', escuchando el topic {args.topic}...")
        client.loop_forever()  # Mantiene la conexión y escucha mensajes

    except KeyboardInterrupt:
        print("\nInterrupción detectada. Cerrando cliente MQTT...")
        client.disconnect()  # Cierra la conexión MQTT de forma segura
        print("Conexión cerrada. Salida limpia.")

    except Exception as e:
        print(f"Error inesperado: {e}")



if __name__ == "__main__":
    main()
