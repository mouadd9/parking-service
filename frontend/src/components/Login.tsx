import { useState } from "react";
import { Mail, Lock, ParkingCircle } from "lucide-react";
import { Header } from "../components/Header";

export const Login = () => {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [rememberMeChecked, setRememberMeChecked] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState(false);

  const basePath = "";

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError("");

    try {
      const response = await fetch("http://localhost:8080/api/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          email,
          password,
          rememberMe: rememberMeChecked,
        }),
      });

      const data = await response.json();

      if (!response.ok) {
        setSuccess(false);
        setError(data.message || "⚠️ Identifiants invalides");
        setLoading(false);
        return;
      }

      // Login réussi
      localStorage.setItem("token", data.token);
      localStorage.setItem("role", data.role);
      setSuccess(true);
    } catch (err) {
      console.error(err);
      setError("⚠️ Erreur serveur, veuillez réessayer plus tard.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen mt-12 bg-slate-50">
      <Header  />

      <div className="flex items-center justify-center px-4 py-12">
        <div className="bg-white/95 backdrop-blur-md shadow-2xl rounded-2xl p-8 w-full max-w-md border border-slate-200">
          <div className="flex flex-col items-center mb-6">
            <div className="bg-sky-600 text-white p-3 rounded-full mb-3 shadow">
              <ParkingCircle size={32} />
            </div>
            <h1 className="text-2xl font-bold text-slate-800">
              Gestion du Parking
            </h1>
            <p className="text-slate-500 text-sm mt-1">
              Connexion à la plateforme de supervision
            </p>
          </div>

          {/* Message succès */}
          {success && (
            <div className="bg-emerald-50 text-emerald-800 border border-emerald-200 p-2 rounded text-sm mb-4 text-center">
              ✅ Connexion réussie
            </div>
          )}

          {/* Message erreur */}
          {error && !success && (
            <div className="bg-amber-50 text-amber-800 border border-amber-200 p-2 rounded text-sm mb-4 text-center">
              {error}
            </div>
          )}

          {/* Formulaire */}
          <form onSubmit={handleSubmit} className="space-y-5">
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">
                Adresse e-mail
              </label>
              <div className="relative">
                <Mail
                  className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400"
                  size={18}
                />
                <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  disabled={success}
                  required
                  className="w-full pl-10 pr-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-sky-500 disabled:bg-slate-100"
                />
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">
                Mot de passe
              </label>
              <div className="relative">
                <Lock
                  className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400"
                  size={18}
                />
                <input
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  disabled={success}
                  required
                  className="w-full pl-10 pr-3 py-2 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-sky-500 disabled:bg-slate-100"
                />
              </div>
            </div>

            

            <button
              type="submit"
              disabled={loading || success}
              className={`w-full bg-sky-600 hover:bg-sky-700 text-white font-semibold py-2 rounded-lg transition duration-300 shadow-lg ${
                loading || success ? "opacity-50 cursor-not-allowed" : ""
              }`}
            >
              {loading
                ? "Connexion..."
                : success
                ? "Connecté"
                : "Se connecter"}
            </button>
          </form>

          <p className="text-center text-xs text-slate-500 mt-6">
            © 2025 Système de Gestion du Parking – Accès sécurisé
          </p>
        </div>
      </div>
    </div>
  );
};
