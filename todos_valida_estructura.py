# ...existing code...

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

# ...existing code...