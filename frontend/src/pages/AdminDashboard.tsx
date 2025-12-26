import { useEffect, useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Users, Car, MapPin, MessageSquare, TrendingUp, Activity, RefreshCw } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { useToast } from '@/hooks/use-toast';
import { adminApi } from '@/services/adminApi';

interface Statistics {
  users: {
    total: number;
    drivers: number;
    admins: number;
  };
  parking: {
    totalZones: number;
    totalSpots: number;
    availableSpots: number;
    occupiedSpots: number;
  };
  sessions: {
    total: number;
    active: number;
    completed: number;
  };
  reclamations: {
    total: number;
  };
}

const AdminDashboard = () => {
  const [statistics, setStatistics] = useState<Statistics | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [syncing, setSyncing] = useState(false);
  const navigate = useNavigate();
  const { toast } = useToast();

  // Auto-sync parking zones on component mount
  useEffect(() => {
    const autoSyncAndFetchStatistics = async () => {
      try {
        setLoading(true);

        // Get auth token from localStorage (Clerk user ID for now)
        const userId = localStorage.getItem('clerk_user_id');
        if (!userId) {
          navigate('/admin/login');
          return;
        }

        // Check if we need to sync (only if no zones exist)
        const zonesResponse = await fetch('http://localhost:8080/api/zones');
        const zones = await zonesResponse.json();

        if (zones.length === 0) {
          // Auto-sync parking zones from Overpass API
          console.log('No parking zones found. Auto-syncing from Overpass API...');
          try {
            const syncResult = await adminApi.syncParkingZones();
            console.log('Auto-sync completed:', syncResult);

            toast({
              title: "Auto-sync Completed",
              description: `Synced ${syncResult.total} parking zones from OpenStreetMap`,
            });
          } catch (syncErr) {
            console.error('Auto-sync failed:', syncErr);
            // Continue even if sync fails
          }
        }

        // Fetch statistics
        const response = await fetch('http://localhost:8080/api/admin/statistics', {
          headers: {
            'Authorization': `Bearer ${userId}`,
          },
        });

        if (response.status === 401 || response.status === 403) {
          navigate('/admin/login');
          return;
        }

        if (!response.ok) {
          throw new Error('Failed to fetch statistics');
        }

        const data = await response.json();
        setStatistics(data);
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Failed to load statistics');
      } finally {
        setLoading(false);
      }
    };

    autoSyncAndFetchStatistics();
  }, [navigate, toast]);

  const handleSyncParkingZones = async () => {
    try {
      setSyncing(true);
      const result = await adminApi.syncParkingZones();

      toast({
        title: "Sync Successful",
        description: `Created: ${result.created}, Updated: ${result.updated}, Total: ${result.total}`,
      });

      // Refresh statistics after sync
      window.location.reload();
    } catch (err) {
      toast({
        title: "Sync Failed",
        description: err instanceof Error ? err.message : 'Failed to sync parking zones',
        variant: "destructive",
      });
    } finally {
      setSyncing(false);
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-background flex items-center justify-center">
        <div className="flex flex-col items-center gap-2">
          <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
          <span className="text-sm text-muted-foreground">Loading statistics...</span>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-screen bg-background flex items-center justify-center">
        <Card className="w-full max-w-md">
          <CardHeader>
            <CardTitle className="text-destructive">Error</CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-sm text-muted-foreground">{error}</p>
          </CardContent>
        </Card>
      </div>
    );
  }

  if (!statistics) {
    return null;
  }

  const occupancyRate = statistics.parking.totalSpots > 0
    ? ((statistics.parking.occupiedSpots / statistics.parking.totalSpots) * 100).toFixed(1)
    : 0;

  return (
    <div className="min-h-screen bg-background">
      <div className="container mx-auto px-4 py-8">
        {/* Header */}
        <div className="mb-8">
          <h1 className="text-4xl font-bold text-foreground mb-2">Admin Dashboard</h1>
          <p className="text-muted-foreground">Parking Service Management Overview</p>
        </div>

        {/* Navigation */}
        <div className="mb-6 flex gap-3 justify-between items-center">
          <div className="flex gap-3">
            <Button onClick={() => navigate('/admin/dashboard')} variant="default">
              Dashboard
            </Button>
            <Button onClick={() => navigate('/admin/reclamations')} variant="outline">
              Reclamations
            </Button>
            <Button onClick={() => navigate('/')} variant="outline">
              User View
            </Button>
          </div>
          <Button
            onClick={handleSyncParkingZones}
            variant="outline"
            disabled={syncing}
            className="flex items-center gap-2"
          >
            <RefreshCw className={`h-4 w-4 ${syncing ? 'animate-spin' : ''}`} />
            {syncing ? 'Syncing...' : 'Sync Parking Zones'}
          </Button>
        </div>

        {/* Statistics Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
          {/* Users Card */}
          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">Total Users</CardTitle>
              <Users className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{statistics.users.total}</div>
              <p className="text-xs text-muted-foreground">
                {statistics.users.drivers} drivers, {statistics.users.admins} admins
              </p>
            </CardContent>
          </Card>

          {/* Parking Zones Card */}
          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">Parking Zones</CardTitle>
              <MapPin className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{statistics.parking.totalZones}</div>
              <p className="text-xs text-muted-foreground">
                {statistics.parking.totalSpots} total spots
              </p>
            </CardContent>
          </Card>

          {/* Active Sessions Card */}
          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">Active Sessions</CardTitle>
              <Activity className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{statistics.sessions.active}</div>
              <p className="text-xs text-muted-foreground">
                {statistics.sessions.completed} completed
              </p>
            </CardContent>
          </Card>

          {/* Reclamations Card */}
          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">Reclamations</CardTitle>
              <MessageSquare className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{statistics.reclamations.total}</div>
              <p className="text-xs text-muted-foreground">Total complaints</p>
            </CardContent>
          </Card>
        </div>

        {/* Detailed Statistics */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          {/* Parking Availability */}
          <Card>
            <CardHeader>
              <CardTitle>Parking Availability</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-4">
                <div className="flex items-center justify-between">
                  <span className="text-sm font-medium">Total Spots</span>
                  <span className="text-sm text-muted-foreground">{statistics.parking.totalSpots}</span>
                </div>
                <div className="flex items-center justify-between">
                  <span className="text-sm font-medium text-green-600">Available</span>
                  <span className="text-sm font-bold text-green-600">{statistics.parking.availableSpots}</span>
                </div>
                <div className="flex items-center justify-between">
                  <span className="text-sm font-medium text-red-600">Occupied</span>
                  <span className="text-sm font-bold text-red-600">{statistics.parking.occupiedSpots}</span>
                </div>
                <div className="pt-4 border-t">
                  <div className="flex items-center justify-between mb-2">
                    <span className="text-sm font-medium">Occupancy Rate</span>
                    <span className="text-sm font-bold">{occupancyRate}%</span>
                  </div>
                  <div className="w-full bg-muted rounded-full h-2">
                    <div
                      className="bg-primary rounded-full h-2 transition-all"
                      style={{ width: `${occupancyRate}%` }}
                    />
                  </div>
                </div>
              </div>
            </CardContent>
          </Card>

          {/* Session Overview */}
          <Card>
            <CardHeader>
              <CardTitle>Session Overview</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-4">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <TrendingUp className="h-4 w-4 text-blue-600" />
                    <span className="text-sm font-medium">Total Sessions</span>
                  </div>
                  <span className="text-sm text-muted-foreground">{statistics.sessions.total}</span>
                </div>
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <Car className="h-4 w-4 text-green-600" />
                    <span className="text-sm font-medium">Active Now</span>
                  </div>
                  <span className="text-sm font-bold text-green-600">{statistics.sessions.active}</span>
                </div>
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <Activity className="h-4 w-4 text-gray-600" />
                    <span className="text-sm font-medium">Completed</span>
                  </div>
                  <span className="text-sm text-muted-foreground">{statistics.sessions.completed}</span>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
};

export default AdminDashboard;
