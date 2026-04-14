# Requiere: pip install pandas openpyxl
import pandas as pd

readme = [
  ["Objetivo", "Guía para construir GTFS-RT desde buses usando Config STM"],
  ["Convenciones", "✅ directo | 🧩 compuesto | ⚠️ faltante"],
  ["Claves", "direction_id A/B→0/1; stop_id desde PARADA_COD; histeresis 3 m; MQTT 2–5 s"],
]

veh_rows = [
  ["entity.id","ID único de entidad","runtime","veh-{empresa}-{bus_id}","⚠️","Estable y único"],
  ["vehicle.trip.trip_id","Viaje GTFS","T19_VARIANTES + GTFS","VARIANTE_COD + start_time → trip_id","🧩","Usa mapeo estático"],
  ["vehicle.trip.route_id","Ruta","T17_LINEAS + GTFS","LINEA_COD/NOMBRE_PUBLICO → route_id","🧩",""],
  ["vehicle.trip.direction_id","Sentido","T19_VARIANTES[].SENTIDO","'A'→0, 'B'→1","🧩",""],
  ["vehicle.trip.start_time","Hora salida","runtime","HH:MM:SS","⚠️",""],
  ["vehicle.vehicle.id","ID vehículo","runtime","ID interno flota","⚠️",""],
  ["vehicle.vehicle.label","Rótulo","runtime","'100 - Int 1234'","⚠️",""],
  ["vehicle.vehicle.license_plate","Matrícula","runtime","—","⚠️",""],
  ["vehicle.position.latitude","Latitud","runtime GNSS","Validar rango","⚠️",""],
  ["vehicle.position.longitude","Longitud","runtime GNSS","Validar rango","⚠️",""],
  ["vehicle.position.bearing","Rumbo","runtime","0–359","⚠️",""],
  ["vehicle.position.speed","Velocidad","runtime","m/s","⚠️",""],
  ["vehicle.position.odometer","Odómetro","runtime","m; monotónico","⚠️",""],
  ["vehicle.current_stop_sequence","Secuencia actual","T3_ETAPAS_VARIANTE","Nearest-stop → ORDINAL/índice","🧩","Con histeresis"],
  ["vehicle.stop_id","Parada","T20_PARADAS.PARADA_COD","Mapeo a stop_id GTFS","🧩","Dentro del radio"],
  ["vehicle.current_status","Estado parada","T39_PARAMETROS_GLOBALES","Distancia/velocidad","🧩",""],
  ["vehicle.timestamp","Epoch UTC","runtime","Validar drift 30s","⚠️",""],
  ["vehicle.congestion_level","Congestión","backend","Derivado velocidad","⚠️","Opcional"],
  ["vehicle.occupancy_status","Ocupación","runtime APC","Enum","⚠️",""],
  ["vehicle.occupancy_percentage","% Ocupación","runtime APC","0–100","⚠️",""],
]

tu_rows = [
  ["entity.id","ID único","runtime","tu-{empresa}-{bus_id}","⚠️",""],
  ["trip.trip_id","Viaje","T19_VARIANTES + GTFS","VARIANTE_COD + start_time → trip_id","🧩",""],
  ["trip.route_id","Ruta","T17_LINEAS + GTFS","LINEA_COD → route_id","🧩",""],
  ["trip.direction_id","Sentido","T19_VARIANTES[].SENTIDO","'A'→0, 'B'→1","🧩",""],
  ["vehicle.id","Vehículo","runtime","—","⚠️",""],
  ["timestamp","Epoch UTC","runtime","—","⚠️",""],
  ["stop_time_update[].stop_id","Parada","T3_ETAPAS_VARIANTE + T20_PARADAS","ORDINAL→lista → stop_id GTFS","🧩",""],
  ["stop_time_update[].stop_sequence","Secuencia","T3_ETAPAS_VARIANTE[].ORDINAL","Normalizar a secuencia GTFS","🧩",""],
  ["stop_time_update[].arrival.time","ETA/arr","runtime/backend","Cálculo central","⚠️",""],
  ["stop_time_update[].arrival.delay","Delay arr","cálculo","ETA − horario","🧩",""],
  ["stop_time_update[].departure.time","ETD/dep","runtime/backend","Cálculo central","⚠️",""],
  ["stop_time_update[].departure.delay","Delay dep","cálculo","ETD − horario","🧩",""],
  ["stop_time_update[].schedule_relationship","Relación","lógica","SCHEDULED/ADDED/CANCELED","⚠️",""],
  ["delay (trip-level)","Atraso viaje","cálculo","Media/último","🧩","Opcional"],
]

al_rows = [
  ["entity.id","ID alerta","backend","—","⚠️",""],
  ["active_period.start/end","Vigencia","backend","Epoch UTC","⚠️",""],
  ["informed_entity.route_id","Ruta","T17_LINEAS + GTFS","route_id","🧩",""],
  ["informed_entity.trip_id","Viaje","T19_VARIANTES + GTFS","trip_id","🧩",""],
  ["informed_entity.stop_id","Parada","T20_PARADAS","stop_id GTFS","🧩",""],
  ["cause","Causa","backend","Enum GTFS","⚠️",""],
  ["effect","Efecto","backend","Enum GTFS","⚠️",""],
  ["header_text","Título","backend","Texto","⚠️",""],
  ["description_text","Descripción","backend","Texto","⚠️",""],
  ["url","URL","backend","Texto","⚠️",""],
  ["severity_level","Severidad","backend","Enum","⚠️",""],
]

with pd.ExcelWriter("GTFS-RT_Mapping_STM.xlsx", engine="openpyxl") as w:
    pd.DataFrame(readme, columns=["Clave","Valor"]).to_excel(w, sheet_name="README", index=False)
    pd.DataFrame(veh_rows, columns=["GTFS-RT Campo","Descripción","Fuente STM","Transformación","Estado","Observaciones"]).to_excel(w, sheet_name="VehiclePosition", index=False)
    pd.DataFrame(tu_rows, columns=["GTFS-RT Campo","Descripción","Fuente STM","Transformación","Estado","Observaciones"]).to_excel(w, sheet_name="TripUpdate", index=False)
    pd.DataFrame(al_rows, columns=["GTFS-RT Campo","Descripción","Fuente STM","Transformación","Estado","Observaciones"]).to_excel(w, sheet_name="Alert", index=False)
print("Generado: GTFS-RT_Mapping_STM.xlsx")
