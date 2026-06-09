import { useState, useEffect, useCallback } from "react";
import Dashboard from "../components/Dashboard.tsx";
import api from "./api.ts";

interface User {
  id: number;
  login: string;
  role: { name: string };
  team: { id: number; name: string } | null;
}

interface Team {
  id: number;
  name: string;
}

function App() {
  const [token, setToken] = useState<string | null>(localStorage.getItem("jwt_token"));
  const [userProfile, setUserProfile] = useState<User | null>(null);
  const [isRegistering, setIsRegistering] = useState(false);

  // Lists
  const [teams, setTeams] = useState<Team[]>([]);

  // Auth Inputs
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [selectedTeamId, setSelectedTeamId] = useState<number | "">("");

  // UI feedback
  const [errorMsg, setErrorMsg] = useState("");
  const [successMsg, setSuccessMsg] = useState("");
  const [loading, setLoading] = useState(false);

  // Decode JWT and fetch profile
  const fetchUserProfile = useCallback(async (jwt: string) => {
    try {
      setLoading(true);
      // Decode JWT payload (middle section) to get subject (userId)
      const payloadBase64 = jwt.split(".")[1];
      const decodedPayload = JSON.parse(atob(payloadBase64));
      const userId = decodedPayload.sub;

      const res = await api.get<User>(`/users/${userId}`);
      setUserProfile(res.data);
      setErrorMsg("");
    } catch (err: any) {
      console.error("Failed to load user profile", err);
      // Token might be corrupted/expired
      handleLogout();
    } finally {
      setLoading(false);
    }
  }, []);

  // Fetch available teams
  const fetchTeams = async () => {
    try {
      const res = await api.get<Team[]>("/teams");
      setTeams(res.data);
      if (res.data.length > 0 && selectedTeamId === "") {
        setSelectedTeamId(res.data[0].id);
      }
    } catch (err) {
      console.error("Failed to fetch teams", err);
    }
  };

  useEffect(() => {
    if (token) {
      fetchUserProfile(token);
    }
    fetchTeams();
  }, [token, fetchUserProfile]);

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!username.trim() || !password.trim()) {
      setErrorMsg("Wprowadź login i hasło.");
      return;
    }
    
    try {
      setLoading(true);
      setErrorMsg("");
      const res = await api.post<{ token: string }>("/auth/login", {
        login: username,
        password,
      });

      const jwt = res.data.token;
      localStorage.setItem("jwt_token", jwt);
      setToken(jwt);
      setUsername("");
      setPassword("");
    } catch (err: any) {
      setErrorMsg(err.response?.data?.message || "Błędny login lub hasło.");
    } finally {
      setLoading(false);
    }
    
  };

  const handleRegister = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!username.trim() || !password.trim() || selectedTeamId === "") {
      setErrorMsg("Wypełnij wszystkie pola formularza.");
      return;
    }

    try {
      setLoading(true);
      setErrorMsg("");
      await api.post("/auth/register", {
        login: username,
        password,
        teamId: Number(selectedTeamId),
      });

      setSuccessMsg("Konto zarejestrowane pomyślnie! Zaloguj się.");
      setIsRegistering(false);
      setPassword("");
    } catch (err: any) {
      setErrorMsg(err.response?.data?.message || "Błąd podczas rejestracji.");
    } finally {
      setLoading(false);
    }
  };

  const handleLogout = async () => {
    try {
      await api.post("/auth/logout");
    } catch (err) {
      // Ignore auth logout errors since token is cleared anyway
    }
    localStorage.removeItem("jwt_token");
    setToken(null);
    setUserProfile(null);
    setSuccessMsg("Wylogowano pomyślnie.");
    setErrorMsg("");
  };

  // Clear messages automatically
  useEffect(() => {
    if (successMsg || errorMsg) {
      const timer = setTimeout(() => {
        setSuccessMsg("");
        setErrorMsg("");
      }, 5000);
      return () => clearTimeout(timer);
    }
  }, [successMsg, errorMsg]);

  if (loading && !userProfile) {
    return (
      <div style={{ display: "flex", justifyContent: "center", alignItems: "center", minHeight: "100vh" }}>
        <h3>Trwa ładowanie profilu zabezpieczeń...</h3>
      </div>
    );
  }

  if (token && userProfile) {
    return <Dashboard userProfile={userProfile} onLogout={handleLogout} />;
  }

  return (
    <div className="auth-container">
      <div className="auth-card">
        <div className="auth-header">
          <h1>KIBALLE</h1>
          <p>{isRegistering ? "Tworzenie nowego profilu" : "Dla prawdziwych fanów"}</p>
        </div>

        {errorMsg && <div className="alert alert-danger">{errorMsg}</div>}
        {successMsg && <div className="alert alert-success">{successMsg}</div>}

        {isRegistering ? (
          /* REGISTRATION FORM */
          <form onSubmit={handleRegister}>
            <div className="form-group">
              <label className="form-label">Nazwa konta (login)</label>
              <input
                type="text"
                className="form-input"
                placeholder="np. cyber_fan"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                required
              />
            </div>
            <div className="form-group">
              <label className="form-label">Hasło dostępowe</label>
              <input
                type="password"
                className="form-input"
                placeholder="••••••••••••"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
              />
            </div>
            <div className="form-group">
              <label className="form-label">Wybierz klub kibica (Drużyna)</label>
              <select
                className="form-input"
                value={selectedTeamId}
                onChange={(e) => setSelectedTeamId(e.target.value === "" ? "" : Number(e.target.value))}
                required
              >
                <option value="" disabled>Wybierz klub...</option>
                {teams.map((t) => (
                  <option key={t.id} value={t.id}>
                    {t.name}
                  </option>
                ))}
              </select>
            </div>
            <button type="submit" className="btn btn-primary" style={{ width: "100%", marginTop: "10px" }} disabled={loading}>
              {loading ? "Rejestrowanie..." : "Zarejestruj konto"}
            </button>
            <div className="auth-footer">
              Masz już konto?{" "}
              <a
                href="#"
                onClick={(e) => {
                  e.preventDefault();
                  setIsRegistering(false);
                  setErrorMsg("");
                }}
              >
                Zaloguj się
              </a>
            </div>
          </form>
        ) : (
          /* LOGIN FORM */
          <form onSubmit={handleLogin}>
            <div className="form-group">
              <label className="form-label">Nazwa konta (login)</label>
              <input
                type="text"
                className="form-input"
                placeholder="np. admin"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                required
              />
            </div>
            <div className="form-group">
              <label className="form-label">Hasło</label>
              <input
                type="password"
                className="form-input"
                placeholder="••••••••••••"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
              />
            </div>
            <button type="submit" className="btn btn-primary" style={{ width: "100%", marginTop: "10px" }} disabled={loading}>
              {loading ? "Weryfikacja..." : "Zaloguj"}
            </button>
            <div className="auth-footer">
              Nie posiadasz jeszcze profilu?{" "}
              <a
                href="#"
                onClick={(e) => {
                  e.preventDefault();
                  setIsRegistering(true);
                  setErrorMsg("");
                }}
              >
                Zarejestruj się
              </a>
            </div>
          </form>
        )}
      </div>
    </div>
  );
}

export default App;
