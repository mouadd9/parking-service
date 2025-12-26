import { ParkingCircle, LogOut } from "lucide-react";
import { useNavigate } from "react-router-dom";

export const Header_Conducteur = () => {
  const navigate = useNavigate();

  const handleLogout = () => {
    // Supprimer les infos d'auth
    localStorage.removeItem("token");
    localStorage.removeItem("role");
    localStorage.removeItem("user");

    // Redirection vers login
    navigate("/login");
  };

  return (
    <header className="w-full h-14 flex items-center justify-between px-6 bg-white border-b shadow-sm">
      {/* Logo */}
      <div className="flex items-center gap-2 text-sky-600">
        <ParkingCircle size={26} />
        <span className="font-semibold text-lg">Parking Manager</span>
      </div>

      {/* Logout */}
      <button
        onClick={handleLogout}
        className="flex items-center gap-2 text-sm text-slate-600 hover:text-red-600 transition"
      >
        <LogOut size={18} />
        Déconnexion
      </button>
    </header>
  );
};
