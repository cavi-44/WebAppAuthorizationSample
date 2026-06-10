import React, { useState, useEffect } from "react";
import type { Team } from "../src/types.ts";

interface RegisterFormProps {
  onRegister: (username: string, pass: string, teamId: number) => Promise<void>;
  onSwitchToLogin: () => void;
  teams: Team[];
  loading: boolean;
}

const RegisterForm: React.FC<RegisterFormProps> = ({ onRegister, onSwitchToLogin, teams, loading }) => {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [selectedTeamId, setSelectedTeamId] = useState<number | "">("");

  // set first team as default
  useEffect(() => {
    if (teams.length > 0 && selectedTeamId === "") {
      setSelectedTeamId(teams[0].id);
    }
  }, [teams, selectedTeamId]);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (username.trim() && password.trim() && selectedTeamId !== "") {
      onRegister(username, password, Number(selectedTeamId));
    }
  };

  return (
    <form onSubmit={handleSubmit}>
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
          onChange={(e) => setSelectedTeamId(Number(e.target.value))}
          required
        >
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
            onSwitchToLogin();
          }}
        >
          Zaloguj się
        </a>
      </div>
    </form>
  );
};

export default RegisterForm;
