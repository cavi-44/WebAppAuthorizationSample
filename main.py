import uvicorn
from fastapi import FastAPI, Depends, HTTPException, status
from datetime import datetime

app = FastAPI()

# ==========================================
# MOCKI (Atrapy danych zamiast bazy Osoby 1)
# ==========================================
MOCK_USERS = {
    "token-jan": {"id": 1, "username": "jan_user", "role": "User"},
    "token-anna": {"id": 2, "username": "anna_mod", "role": "Moderator"},
    "token-szef": {"id": 3, "username": "szef_admin", "role": "Admin"}
}

MOCK_RESOURCES = {
    101: {"id": 101, "title": "Raport finansowy", "author_id": 1},  # Własność Jana
    102: {"id": 102, "title": "Notatka służbowa", "author_id": 2}  # Własność Anny
}


def get_current_user(token: str = "token-jan"):
    """
    Symulacja funkcji Osoby 1. Odbiera token i zwraca słownik z użytkownikiem.
    Do testów możesz zmieniać domyślną wartość tokenu w argumentach.
    """
    user = MOCK_USERS.get(token)
    if not user:
        raise HTTPException(status_code=401, detail="Nieprawidłowy token lub brak autoryzacji")
    return user


# ==========================================
# ZADANIE 2.1: RBAC (Kontrola dostępu oparta na rolach)
# ==========================================
class RoleChecker:
    def __init__(self, allowed_roles: list[str]):
        self.allowed_roles = allowed_roles

    def __call__(self, current_user: dict = Depends(get_current_user)):
        # Sprawdzamy, czy rola użytkownika z tokenu jest na liście dozwolonych ról
        if current_user["role"] not in self.allowed_roles:
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail=f"RBAC: Brak dostępu. Twoja rola to '{current_user['role']}', wymagana to jedna z: {self.allowed_roles}"
            )
        return current_user


# ==========================================
# ZADANIE 2.2: ABAC (Atrybuty: Właściciel zasobu + Czas środowiska)
# ==========================================
def verify_business_hours():
    current_hour = datetime.now().hour
    if not (8 <= current_hour < 19):
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="ABAC: Edycja zasobów jest możliwa tylko w godzinach pracy (8:00 - 16:00)."
        )


def verify_ownership(resource_id: int, current_user: dict = Depends(get_current_user)):
    # Pobieramy zasób (w pełnej wersji aplikacji tu będzie query do SQL)
    resource = MOCK_RESOURCES.get(resource_id)
    if not resource:
        raise HTTPException(status_code=404, detail="Zasób nie istnieje.")

    # Sprawdzamy atrybut relacji: czy ID logującego się to ID autora
    if resource["author_id"] != current_user["id"]:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail=f"ABAC: Nie jesteś właścicielem tego zasobu (Twoje ID: {current_user['id']}, Autor ID: {resource['author_id']})."
        )
    return resource


# ==========================================
# ZADANIE 2.3: Zabezpieczenie Endpointów API
# ==========================================

# 1. Endpoint dostępny dla każdego zalogowanego
@app.get("/api/dashboard")
def view_dashboard(user: dict = Depends(get_current_user)):
    return {"message": "Witaj w systemie!", "user": user["username"]}


# 2. Endpoint tylko dla Administratora i Moderatora (Użycie RBAC)
@app.get("/api/admin-panel", dependencies=[Depends(RoleChecker(["Admin", "Moderator"]))])
def view_admin_panel():
    return {"secret_data": "Oto dane widoczne tylko dla wyższych ról."}


# 3. Endpoint z podwójną weryfikacją ABAC (Właściciel + Godziny)
@app.put("/api/resources/{resource_id}", dependencies=[Depends(verify_business_hours)])
def edit_resource(resource: dict = Depends(verify_ownership)):
    # Jeśli użytkownik przejdzie funkcję verify_ownership i verify_business_hours, kod wykona się dalej
    return {"message": "Sukces! Edytujesz swój zasób.", "resource": resource}

if __name__ == "__main__":
    uvicorn.run("main:app", host="127.0.0.1", port=8000, reload=True)