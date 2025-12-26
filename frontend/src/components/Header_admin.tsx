import { Link, useNavigate } from "react-router-dom";
import { ParkingCircle, LogOut } from "lucide-react";

export const Header_admin = () => {
  const navigate = useNavigate();

  const handleLogout = () => {
    localStorage.removeItem("token");
    localStorage.removeItem("role");
    localStorage.removeItem("user");
    navigate("/login");
  };

  console.log("les informations de user est", localStorage.getItem("user"));

  return (
    <header className="fixed top-0 left-0 right-0 z-50 bg-white/95 backdrop-blur-sm border-b border-slate-200">
      <div className="flex items-center justify-between h-16 px-6">

        {/* Logo / Titre (à gauche) */}
        <Link
          to="/"
          className="flex items-center gap-3 hover:opacity-80 transition-opacity"
        >
          <div className="bg-sky-600 text-white p-2 rounded-lg shadow">
            <ParkingCircle className="h-6 w-6" />
          </div>
          <span className="text-xl font-bold text-slate-800">
            Parking Manager
          </span>
        </Link>

        

        {/* Bouton Déconnexion (à droite) */}
        <button
          onClick={handleLogout}
          className="flex items-center gap-2 text-sm font-medium text-slate-600 hover:text-red-600 transition"
        >
          <LogOut size={18} />
          Déconnexion
        </button>

      </div>
    </header>
  );
};

export default Header_admin;
