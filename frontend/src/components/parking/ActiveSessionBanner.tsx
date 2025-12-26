// ============================================================================
// 1. MODIFIER ActiveSessionBanner.tsx - Retirer le bouton Stop
// ============================================================================
// components/parking/ActiveSessionBanner.tsx

import { useEffect, useState } from "react";
import { Card, CardContent } from "@/components/ui/card";
import { Clock, MapPin } from "lucide-react";
import { ParkingSession } from "@/types/parking";

interface ActiveSessionBannerProps {
  session: ParkingSession;
  hourlyRate: number;
}

export default function ActiveSessionBanner({
  session,
  hourlyRate,
}: ActiveSessionBannerProps) {
  const [elapsedTime, setElapsedTime] = useState(0);
  const [currentCost, setCurrentCost] = useState(0);

  useEffect(() => {
    if (session.status === "reserved" || !session.startTime) {
      // Session réservée mais pas encore commencée
      setElapsedTime(0);
      setCurrentCost(0);
      return;
    }

    // Timer pour mettre à jour le temps écoulé chaque seconde
    const interval = setInterval(() => {
      const now = new Date().getTime();
      const start = new Date(session.startTime!).getTime();
      const elapsed = Math.floor((now - start) / 1000); // en secondes
      
      setElapsedTime(elapsed);
      
      // Calculer le coût en temps réel
      const hours = elapsed / 3600;
      const cost = hours * hourlyRate;
      setCurrentCost(Math.max(cost, hourlyRate)); // Minimum = 1 heure
    }, 1000);

    return () => clearInterval(interval);
  }, [session.startTime, session.status, hourlyRate]);

  const formatTime = (seconds: number) => {
    const hrs = Math.floor(seconds / 3600);
    const mins = Math.floor((seconds % 3600) / 60);
    const secs = seconds % 60;
    return `${hrs.toString().padStart(2, "0")}:${mins
      .toString()
      .padStart(2, "0")}:${secs.toString().padStart(2, "0")}`;
  };

  return (
    <Card className="w-full shadow-lg border-2 border-primary">
      <CardContent className="p-4">
        <div className="flex flex-col gap-3">
          {/* Header */}
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <MapPin className="w-5 h-5 text-primary" />
              <div>
                <p className="font-semibold text-sm">{session.zoneName}</p>
                <p className="text-xs text-muted-foreground">
                  Spot {session.spotNumber}
                </p>
              </div>
            </div>
            
            {/* Status Badge */}
            <div
              className={`px-3 py-1 rounded-full text-xs font-semibold ${
                session.status === "active"
                  ? "bg-green-500/20 text-green-700 border border-green-500/30"
                  : "bg-blue-500/20 text-blue-700 border border-blue-500/30"
              }`}
            >
              {session.status === "active" ? "⏱️ Active" : "📍 Reserved"}
            </div>
          </div>

          {/* Timer & Cost */}
          {session.status === "active" && session.startTime && (
            <>
              <div className="border-t pt-3">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <Clock className="w-4 h-4 text-primary" />
                    <span className="text-sm text-muted-foreground">
                      Duration
                    </span>
                  </div>
                  <span className="text-xl font-mono font-bold">
                    {formatTime(elapsedTime)}
                  </span>
                </div>
              </div>

              <div className="border-t pt-3">
                <div className="flex items-center justify-between">
                  <span className="text-sm text-muted-foreground">
                    Current Cost
                  </span>
                  <span className="text-2xl font-bold text-primary">
                    {currentCost.toFixed(2)}€
                  </span>
                </div>
                <p className="text-xs text-muted-foreground text-right mt-1">
                  {hourlyRate.toFixed(2)}€/hour
                </p>
              </div>
            </>
          )}

          {/* Waiting Message */}
          {session.status === "reserved" && (
            <div className="border-t pt-3">
              <div className="flex items-center gap-2 text-blue-600">
                <Clock className="w-4 h-4 animate-pulse" />
                <p className="text-sm font-medium">
                  Waiting for entry detection...
                </p>
              </div>
              <p className="text-xs text-muted-foreground mt-2">
                Drive to the parking spot. Timer will start automatically when
                the sensor detects your vehicle.
              </p>
            </div>
          )}

          {/* 🔥 AUTOMATIC EXIT DETECTION MESSAGE 🔥 */}
          {session.status === "active" && (
            <div className="border-t pt-3">
              <div className="flex items-center gap-2 text-amber-600">
                <div className="w-2 h-2 bg-amber-500 rounded-full animate-pulse" />
                <p className="text-xs font-medium">
                  Timer will stop automatically when you exit
                </p>
              </div>
            </div>
          )}
        </div>
      </CardContent>
    </Card>
  );
}