import json
import paho.mqtt.client as mqtt

# Callback cuando se conecta al broker
def on_connect(client, userdata, flags, rc):
    print(f"Conectado con resultado: {rc}")
    # Suscribirse a todos los tópicos
    #client.subscribe("#", qos=2)

    # Suscribirse a los tópicos que comienzan con STMBuses/50/857
    client.subscribe("STMBuses/50/857", qos=2)

# Lista para almacenar mensajes JSON recibidos
mensajes_recibidos = []

# Cargar el JSON esperado
with open('json_prueba.json', 'r') as f:
    json_esperado = json.load(f)

# Callback cuando se recibe un mensaje
def on_message(client, userdata, message):
    # Decodificar el mensaje recibido
    mensaje_recibido = message.payload.decode()
    
    # Convertir el mensaje a JSON
    try:
        json_recibido = json.loads(mensaje_recibido)
        mensajes_recibidos.append(json_recibido)  # Agregar a la lista
    except json.JSONDecodeError:
        print("Error al decodificar el mensaje JSON")


# Callback cuando se recibe un mensaje
#def on_message(client, userdata, message):
#    print(f"Mensaje recibido en {message.topic}: {message.payload.decode()}")

# Crear un cliente MQTT
client = mqtt.Client()

# Asignar los callbacks
client.on_connect = on_connect
client.on_message = on_message

# Configurar autenticación (si es necesaria)
client.username_pw_set(username="STM-admin", password="pipo")

# Conectar al broker stage
#client.connect("broker-stm-stag.apps.ocp4-desa.imm.gub.uy", 1883, 60)

# Conectar al broker local de homologación
client.connect("stm30testv.imm.gub.uy", 1883, 60)

# Iniciar el bucle de red
client.loop_start()


# Función para comparar la lista de mensajes con el JSON esperado
def comparar_mensajes(json_esperado, mensajes_recibidos):
    # Combinar los mensajes recibidos en un solo JSON
    json_combinado = mensajes_recibidos  # Estructura combinada

    # Normalizar ambos JSON a cadenas
    json_esperado_str = json.dumps(json_esperado, sort_keys=True)
    json_combinado_str = json.dumps(json_combinado, sort_keys=True)

    # Comparar las cadenas JSON
    if json_combinado_str == json_esperado_str:
        print("Los mensajes recibidos coinciden con el JSON esperado.")
    else:
        print("Los mensajes recibidos no coinciden con el JSON esperado.")
        print("JSON esperado:")
        print(json_esperado_str)
        print("JSON combinado:")
        print(json_combinado_str)
      
# Puedes llamar a esta función en un momento específico
# Por ejemplo, si decides que después de un tiempo o cierto número de mensajes
try:
    while True:
        pass  # Mantener el script en ejecución
except KeyboardInterrupt:
    print("Interrumpido por el usuario")
    print("Mensajes recibidos:")
    print(json.dumps(mensajes_recibidos, indent=4))  # Imprimir en formato JSON
    # Comparar los mensajes recibidos con el JSON esperado
    comparar_mensajes(json_esperado, mensajes_recibidos)
finally:
    client.loop_stop()
    client.disconnect()
