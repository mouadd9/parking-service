import { Link } from "react-router-dom";
import { ParkingCircle } from "lucide-react";

export const Header = () => {
  return (
    <header className="fixed top-0 left-0 right-0 z-50 bg-white/95 backdrop-blur-sm border-b border-slate-200">
      <div className="flex items-center justify-center h-16 px-6">
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
      </div>
    </header>
  );
};
