"""
Заполнение Firestore станциями по схеме клуба VENOM (22 места).
Требует: pip install firebase-admin
Ключ сервис-аккаунта: скачай в Firebase Console -> Project Settings -> Service accounts
и сохрани как serviceAccountKey.json рядом с этим файлом.

Запуск: python seed_stations.py
"""
import firebase_admin
from firebase_admin import credentials, firestore

cred = credentials.Certificate("serviceAccountKey.json")
firebase_admin.initialize_app(cred)
db = firestore.client()

stations = []
# ПК 1-10 — верхний ряд (жёлтые на схеме)
for n in range(1, 11):
    stations.append(dict(number=n, title=f"PC {n}", type="PC", zone="Общий зал"))
# ПК 11-14 — оранжевый ряд (мощные)
for n in range(11, 15):
    stations.append(dict(number=n, title=f"PC {n} PRO", type="PC", zone="PRO ряд"))
# ПК 15-17 — нижний ряд
for n in range(15, 18):
    stations.append(dict(number=n, title=f"PC {n}", type="PC", zone="Общий зал"))
# Консоли 18-21 — средний ряд
for n in range(18, 22):
    stations.append(dict(number=n, title=f"Консоль {n}", type="PS5", zone="Консоли"))
# 22 — VIP-место
stations.append(dict(number=22, title="VIP 22", type="PC", zone="VIP"))
# PS5 комнаты 23-26
for n in range(23, 27):
    stations.append(dict(number=n, title=f"{n-22} комната", type="PS5", zone="PS5 комнаты"))

for s in stations:
    s.update(status="FREE", statusNote="", bookedUntil=None, bookedBy="", gizmoHostId=None)
    db.document(f"stations/st{s['number']:02d}").set(s)
    print("OK", s["title"])

print(f"\nГотово: {len(stations)} станций.")
