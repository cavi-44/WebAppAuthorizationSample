import { useState, useEffect, useCallback } from "react";
import Dashboard from "../components/Dashboard.tsx";
import LoginForm from "../components/LoginForm.tsx";
import RegisterForm from "../components/RegisterForm.tsx";
import api from "./api.ts";
import type { User, Team } from "./types.ts";

function App() {
  const [token, setToken] = useState<string | null>(
    localStorage.getItem("jwt_token"),
  );
  const [userProfile, setUserProfile] = useState<User | null>(null);
  const [isRegistering, setIsRegistering] = useState(false);
  const [teams, setTeams] = useState<Team[]>([]);
  const [errorMsg, setErrorMsg] = useState("");
  const [successMsg, setSuccessMsg] = useState("");
  const [loading, setLoading] = useState(false);

  // fetch profile using jwt
  const fetchUserProfile = useCallback(async (jwt: string) => {
    try {
      setLoading(true);
      // extract user id from jwt
      const payloadBase64 = jwt.split(".")[1];
      const decodedPayload = JSON.parse(atob(payloadBase64));
      const userId = decodedPayload.sub;

      const res = await api.get<User>(`/users/${userId}`);
      setUserProfile(res.data);
      setErrorMsg("");
    } catch (err: any) {
      console.error("failed to load profile", err);
      handleLogout();
    } finally {
      setLoading(false);
    }
  }, []);

  const fetchTeams = async () => {
    try {
      const res = await api.get<Team[]>("/teams");
      setTeams(res.data);
    } catch (err) {
      console.error("failed to fetch teams", err);
    }
  };

  useEffect(() => {
    if (token) {
      fetchUserProfile(token);
    }
    fetchTeams();
  }, [token, fetchUserProfile]);

  const handleLogin = async (usernameInput: string, passwordInput: string) => {
    try {
      setLoading(true);
      setErrorMsg("");
      const res = await api.post<{ token: string }>("/auth/login", {
        login: usernameInput,
        password: passwordInput,
      });

      const jwt = res.data.token;
      localStorage.setItem("jwt_token", jwt);
      setToken(jwt);
    } catch (err: any) {
      setErrorMsg(err.response?.data?.message || "bledny login lub haslo");
    } finally {
      setLoading(false);
    }
  };

  const handleRegister = async (
    usernameInput: string,
    passwordInput: string,
    teamIdInput: number,
  ) => {
    try {
      setLoading(true);
      setErrorMsg("");
      await api.post("/auth/register", {
        login: usernameInput,
        password: passwordInput,
        teamId: teamIdInput,
      });

      setSuccessMsg("konto zarejestrowane! mozesz sie zalogowac");
      setIsRegistering(false);
    } catch (err: any) {
      setErrorMsg(err.response?.data?.message || "blad podczas rejestracji");
    } finally {
      setLoading(false);
    }
  };

  const handleLogout = async () => {
    try {
      await api.post("/auth/logout");
    } catch (err) {
      // ignore error, clear state anyway
    }
    localStorage.removeItem("jwt_token");
    setToken(null);
    setUserProfile(null);
    setSuccessMsg("wylogowano pomyslnie");
    setErrorMsg("");
  };

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
      <div
        style={{
          display: "flex",
          justifyContent: "center",
          alignItems: "center",
          minHeight: "100vh",
        }}
      >
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
          <h1>KIBALL</h1>
          <p>{isRegistering ? "Tworzenie nowego profilu" : "Zaloguj sie"}</p>
        </div>

        {errorMsg && <div className="alert alert-danger">{errorMsg}</div>}
        {successMsg && <div className="alert alert-success">{successMsg}</div>}

        {isRegistering ? (
          <RegisterForm
            onRegister={handleRegister}
            onSwitchToLogin={() => {
              setIsRegistering(false);
              setErrorMsg("");
            }}
            teams={teams}
            loading={loading}
          />
        ) : (
          <LoginForm
            onLogin={handleLogin}
            onSwitchToRegister={() => {
              setIsRegistering(true);
              setErrorMsg("");
            }}
            loading={loading}
          />
        )}
      </div>
    </div>
  );
}

export default App;
