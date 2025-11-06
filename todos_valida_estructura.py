import json
import paho.mqtt.client as mqtt

# Definición de la estructura esperada para los diferentes tópicos
EstructuraEsperada = {
    "EmpresaBus": {
        "Version": int,
        "MDTEntidad": str,
        "Latitud": float,
        "Longitud": float,
        "Odometro": int,
        "EmpresaCod": int,
        "BusNro": int,
        "Matricula": str,
        "TipoEmisiones": int,
        "PasajeMaxSalon": int,
        "CantPuertas": int
    },
    "EstadoServicio": {
        "Version": int,
        "MDTEntidad": str,
        "Latitud": float,
        "Longitud": float,
        "Odometro": int,
        "EmpresaCod": int,
        "BusNro": int,
        "EstadoServicioCod": int,
        "NumeroServicio": int,
        "TipoMinuta": int,
        "TipoInicio": int,
        "IdInternoOperador": int
    },
    "EstadoOperativo": {
        "Version": int,
        "MDTEntidad": str,
        "Latitud": float,
        "Longitud": float,
        "Odometro": int,
        "EmpresaCod": int,
        "BusNro": int,
        "EstadoOperativoCodigo": int,
        "VarianteCodigo": int,
        # FechaHoraSegúnMinuta es opcional
    },
    "Parada": {
        "Version": int,
        "MDTEntidad": str,
        "Latitud": float,
        "Longitud": float,
        "Odometro": int,
        "EmpresaCod": int,
        "BusNro": int,
        "ParadaCod": int,
        "ProximaParadaCod": int,
        "NumeroSAM": int,
        # SecuencialSAM es opcional
    }
}

# Función modificada para validar la estructura del mensaje
def validar_mensaje(mensaje, estructura_esperada):
    validado = True
    for campo, tipo_dato in estructura_esperada.items():
        if campo not in mensaje:
            print(f"Campo faltante en el mensaje: {campo}")
            validado = False
        elif not isinstance(mensaje[campo], tipo_dato):
            print(f"Tipo incorrecto para {campo}: se esperaba {tipo_dato.__name__}, se recibió {type(mensaje[campo]).__name__}")
            validado = False
            
    # Validación adicional para campos opcionales
    if "FechaHoraSegúnMinuta" in mensaje and not isinstance(mensaje["FechaHoraSegúnMinuta"], str):
        print(f"Tipo incorrecto para FechaHoraSegúnMinuta: se esperaba str, se recibió {type(mensaje['FechaHoraSegúnMinuta']).__name__}")
        validado = False
        
    if "SecuencialSAM" in mensaje and not isinstance(mensaje["SecuencialSAM"], int):
        print(f"Tipo incorrecto para SecuencialSAM: se esperaba int, se recibió {type(mensaje['SecuencialSAM']).__name__}")
        validado = False
    
    return validado

# Callback cuando se conecta al broker
def on_connect(client, userdata, flags, rc):
    print(f"Conectado con resultado: {rc}")
    # Suscribirse a todos los tópicos de la empresa 50
    client.subscribe("STMBuses/50/#", qos=2)

# Callback cuando se recibe un mensaje
def on_message(client, userdata, message):
    # Decodificar el mensaje recibido
    try:
        mensaje_recibido = message.payload.decode()
        json_recibido = json.loads(mensaje_recibido)

        print("\nNuevo mensaje recibido:")
        print(f"Tópico: {message.topic}")
        print(json.dumps(json_recibido, indent=4))

        # Extraer el tipo de mensaje basado en el tópico
        for tipo_mensaje in EstructuraEsperada.keys():
            if tipo_mensaje in message.topic:
                if validar_mensaje(json_recibido, EstructuraEsperada[tipo_mensaje]):
                    print(f"El mensaje de {tipo_mensaje} tiene la estructura y contenido esperados.")
                else:
                    print(f"El mensaje de {tipo_mensaje} no cumple con la estructura o contenido esperados.")
                break  # Salir del bucle ya que se encontró el tópico correspondiente

    except json.JSONDecodeError:
        print("Error al decodificar el mensaje JSON")
    except Exception as e:
        print(f"Error: {e}")

# Crear un cliente MQTT
client = mqtt.Client()

# Asignar los callbacks
client.on_connect = on_connect
client.on_message = on_message

# Configurar autenticación (descomentando si es necesario)
client.username_pw_set(username="STM-admin", password="pipo")

# Conectar al broker preproducción
# client.connect("broker-stm-preprod.apps.ocp4-prod.imm.gub.uy", 1883, 60)

# Conectar al broker local homologación
client.connect("stm30testv", 1883, 60)

# Iniciar el bucle de red
client.loop_start()

# Iniciar el bucle de red
if __name__ == '__main__':
    client.loop_start()
    try:
        while True:
            pass  # Mantener el script en ejecución
    except KeyboardInterrupt:
        print("\nInterrumpido por el usuario")
    finally:
        client.loop_stop()
        client.disconnect()
