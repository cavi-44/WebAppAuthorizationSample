import React, { useState } from "react";

interface LoginFormProps {
  onLogin: (username: string, pass: string) => Promise<void>;
  onSwitchToRegister: () => void;
  loading: boolean;
}

const LoginForm: React.FC<LoginFormProps> = ({ onLogin, onSwitchToRegister, loading }) => {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (username.trim() && password.trim()) {
      onLogin(username, password);
    }
  };

  return (
    <form onSubmit={handleSubmit}>
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
        {loading ? "Weryfikacja..." : "Uwierzytelnij"}
      </button>
      <div className="auth-footer">
        Nie posiadasz jeszcze profilu?{" "}
        <a
          href="#"
          onClick={(e) => {
            e.preventDefault();
            onSwitchToRegister();
          }}
        >
          Zarejestruj się
        </a>
      </div>
    </form>
  );
};

export default LoginForm;
