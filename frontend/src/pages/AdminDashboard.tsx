import { useEffect, useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Users, Car, MapPin, MessageSquare, TrendingUp, Activity, RefreshCw, AlertCircle, DollarSign, Edit, Save, X } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { useToast } from '@/hooks/use-toast';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Input } from '@/components/ui/input';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Badge } from '@/components/ui/badge';
import {Header_admin} from '../components/Header_admin';

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
    pending: number;
    resolved: number;
  };
  financial?: {
    totalRevenue: number;
    todayRevenue: number;
  };
}

interface ZoneRate {
  id: number;
  name: string;
  currentRate: number;
  proposedRate?: number;
  capacity: number;
  occupiedSpots: number;
  averageDailyRevenue: number;
}

const AdminDashboard = () => {
  const [statistics, setStatistics] = useState<Statistics | null>(null);
  const [zoneRates, setZoneRates] = useState<ZoneRate[]>([]);
  const [editingZoneId, setEditingZoneId] = useState<number | null>(null);
  const [editedRate, setEditedRate] = useState<number>(0);
  const [loading, setLoading] = useState(true);
  const [ratesLoading, setRatesLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [syncing, setSyncing] = useState(false);
  const navigate = useNavigate();
  const { toast } = useToast();

  useEffect(() => {
    const fetchData = async () => {
      await Promise.all([
        fetchStatistics(),
        fetchZoneRates()
      ]);
    };

    fetchData();
  }, []);

  const fetchStatistics = async () => {
    try {
      setLoading(true);
      setError(null);

      const response = await fetch('http://localhost:8080/api/admin/statistics');

      if (!response.ok) {
        if (response.status === 404) {
          console.warn('API endpoint not found, using mock data');
          const mockData = getMockStatistics();
          setStatistics(mockData);
          return;
        }
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const contentType = response.headers.get("content-type");
      if (!contentType || !contentType.includes("application/json")) {
        throw new Error("La réponse n'est pas du JSON");
      }

      const result = await response.json();

      if (result.success) {
        setStatistics(result.statistics);
      } else {
        throw new Error(result.message || 'Failed to fetch statistics');
      }
    } catch (err) {
      console.error('Error fetching statistics:', err);
      setError(err instanceof Error ? err.message : 'Failed to load statistics');

      const mockData = getMockStatistics();
      setStatistics(mockData);
    } finally {
      setLoading(false);
    }
  };

  const fetchZoneRates = async () => {
    try {
      setRatesLoading(true);
      const response = await fetch('http://localhost:8080/api/zones/rates');

      if (!response.ok) {
        if (response.status === 404) {
          // Endpoint non trouvé, utilisez des données mockées
          console.warn('Zone rates endpoint not found, using mock data');
          const mockRates = getMockZoneRates();
          setZoneRates(mockRates);
          return;
        }
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const result = await response.json();
      if (result.success) {
        setZoneRates(result.data || result.rates);
      }
    } catch (err) {
      console.error('Error fetching zone rates:', err);
      // Fallback to mock data
      const mockRates = getMockZoneRates();
      setZoneRates(mockRates);
    } finally {
      setRatesLoading(false);
    }
  };

  const getMockStatistics = (): Statistics => {
    return {
      users: {
        total: 150,
        drivers: 145,
        admins: 5
      },
      parking: {
        totalZones: 5,
        totalSpots: 600,
        availableSpots: 350,
        occupiedSpots: 250
      },
      sessions: {
        total: 1250,
        active: 45,
        completed: 1205
      },
      reclamations: {
        total: 23,
        pending: 5,
        resolved: 18
      },
      financial: {
        totalRevenue: 12560.50,
        todayRevenue: 845.25
      }
    };
  };

  const getMockZoneRates = (): ZoneRate[] => {
    return [
      { id: 1, name: 'Zone A - Centre', currentRate: 5.0, capacity: 100, occupiedSpots: 75, averageDailyRevenue: 1200 },
      { id: 2, name: 'Zone B - Commerciale', currentRate: 4.5, capacity: 80, occupiedSpots: 60, averageDailyRevenue: 850 },
      { id: 3, name: 'Zone C - Résidentielle', currentRate: 3.5, capacity: 120, occupiedSpots: 90, averageDailyRevenue: 950 },
      { id: 4, name: 'Zone D - Périphérie', currentRate: 2.5, capacity: 150, occupiedSpots: 50, averageDailyRevenue: 400 },
      { id: 5, name: 'Zone E - Aéroport', currentRate: 7.0, capacity: 200, occupiedSpots: 150, averageDailyRevenue: 2800 },
    ];
  };

  const handleEditRate = (zone: ZoneRate) => {
    setEditingZoneId(zone.id);
    setEditedRate(zone.currentRate);
  };

  const handleSaveRate = async (zoneId: number) => {
    try {
      const response = await fetch(`http://localhost:8080/api/zones/${zoneId}/rate`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ hourlyRate: editedRate }),
      });

      if (!response.ok) {
        throw new Error('Failed to update rate');
      }

      // Mettre à jour localement
      setZoneRates(prev =>
        prev.map(zone =>
          zone.id === zoneId ? { ...zone, currentRate: editedRate } : zone
        )
      );

      setEditingZoneId(null);

      toast({
        title: "Tarif mis à jour",
        description: `Le tarif a été mis à jour avec succès`,
        variant: "default",
      });

      // Rafraîchir les statistiques financières
      fetchStatistics();

    } catch (err) {
      toast({
        title: "Erreur",
        description: err instanceof Error ? err.message : 'Failed to update rate',
        variant: "destructive",
      });
    }
  };

  const handleCancelEdit = () => {
    setEditingZoneId(null);
  };

  const handleApplyRateIncrease = (percentage: number) => {
    const updatedRates = zoneRates.map(zone => ({
      ...zone,
      proposedRate: parseFloat((zone.currentRate * (1 + percentage / 100)).toFixed(2))
    }));
    setZoneRates(updatedRates);

    toast({
      title: "Ajustement appliqué",
      description: `Augmentation de ${percentage}% appliquée à tous les tarifs`,
    });
  };

  const handleSyncParkingZones = async () => {
    try {
      setSyncing(true);
      const mockResult = {
        created: 10,
        updated: 5,
        total: 15
      };

      toast({
        title: "Sync Successful",
        description: `Created: ${mockResult.created}, Updated: ${mockResult.updated}, Total: ${mockResult.total}`,
      });

      // Rafraîchir les données
      await Promise.all([fetchStatistics(), fetchZoneRates()]);
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

  const calculateAverageRate = () => {
    if (zoneRates.length === 0) return 0;
    const total = zoneRates.reduce((sum, zone) => sum + zone.currentRate, 0);
    return (total / zoneRates.length).toFixed(2);
  };

  const calculateTotalDailyRevenue = () => {
    return zoneRates.reduce((sum, zone) => sum + zone.averageDailyRevenue, 0).toFixed(2);
  };

  if (loading || ratesLoading) {
    return (
      <div className="min-h-screen bg-background flex items-center justify-center">
        <div className="flex flex-col items-center gap-2">
          <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
          <span className="text-sm text-muted-foreground">Loading dashboard...</span>
        </div>
      </div>
    );
  }

  if (error && !statistics) {
    return (
      <div className="min-h-screen bg-background flex items-center justify-center">
        
        <Card className="w-full max-w-md">
          <CardHeader>
            <CardTitle className="text-destructive">Error</CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-sm text-muted-foreground">{error}</p>
            <Button
              onClick={() => window.location.reload()}
              className="mt-4"
            >
              Retry
            </Button>
          </CardContent>
        </Card>
      </div>
    );
  }

  if (!statistics || !zoneRates) {
    return null;
  }

  const occupancyRate = statistics.parking.totalSpots > 0
    ? ((statistics.parking.occupiedSpots / statistics.parking.totalSpots) * 100).toFixed(1)
    : 0;

  return (
    <div className="min-h-screen bg-background">
                <Header_admin />

      <div className="container mx-auto px-4 py-8">
        {/* Header */}
        <div className="mb-8">
          <h1 className="text-4xl font-bold text-foreground mb-2">Admin Dashboard</h1>
          <p className="text-muted-foreground">Parking Service Management Overview</p>
        </div>

        {/* Avertissement si données mockées */}
        {error && (
          <Alert className="mb-6 bg-yellow-50 border-yellow-200">
            <AlertCircle className="h-4 w-4 text-yellow-600" />
            <AlertDescription className="text-yellow-800">
              Using demo data. Backend API may not be available.
            </AlertDescription>
          </Alert>
        )}

        {/* Navigation */}
        <div className="mb-6 flex gap-3 justify-between items-center">
          <div className="flex gap-3">
            <Button onClick={() => navigate('/admin/dashboard')} variant="default">
              Dashboard
            </Button>
            <Button onClick={() => navigate('/admin/reclamations')} variant="outline">
              Reclamations
            </Button>
            <Button onClick={() => navigate('/admin/rates')} variant="outline">
              Tarifs
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

          {/* Average Rate Card */}
          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">Avg. Hourly Rate</CardTitle>
              <DollarSign className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">${calculateAverageRate()}</div>
              <p className="text-xs text-muted-foreground">
                Across {zoneRates.length} zones
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
              <p className="text-xs text-muted-foreground">
                {statistics.reclamations.pending} pending, {statistics.reclamations.resolved} resolved
              </p>
            </CardContent>
          </Card>
        </div>

        {/* Detailed Statistics */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-8">
          {/* Parking Availability */}
          <Card className="lg:col-span-2">
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

          {/* Rate Summary */}
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <DollarSign className="h-5 w-5" />
                Tarifs Summary
              </CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-4">
                <div className="flex items-center justify-between">
                  <span className="text-sm font-medium">Average Hourly Rate</span>
                  <span className="text-sm font-bold">${calculateAverageRate()}</span>
                </div>
                <div className="flex items-center justify-between">
                  <span className="text-sm font-medium">Highest Rate</span>
                  <span className="text-sm font-bold text-green-600">
                    ${Math.max(...zoneRates.map(z => z.currentRate)).toFixed(2)}
                  </span>
                </div>
                <div className="flex items-center justify-between">
                  <span className="text-sm font-medium">Lowest Rate</span>
                  <span className="text-sm font-bold text-blue-600">
                    ${Math.min(...zoneRates.map(z => z.currentRate)).toFixed(2)}
                  </span>
                </div>
                <div className="pt-4 border-t">
                  <div className="flex items-center justify-between mb-2">
                    <span className="text-sm font-medium">Est. Daily Revenue</span>
                    <span className="text-sm font-bold">${calculateTotalDailyRevenue()}</span>
                  </div>
                  <div className="text-xs text-muted-foreground">
                    Based on current occupancy and rates
                  </div>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>

        {/* Zone Rates Management */}
        <Card className="mb-8">
          <CardHeader>
            <div className="flex justify-between items-center">
              <CardTitle className="flex items-center gap-2">
                <DollarSign className="h-5 w-5" />
                Gestion des Tarifs par Zone
              </CardTitle>
              <div className="flex gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => handleApplyRateIncrease(5)}
                >
                  +5% All
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => handleApplyRateIncrease(10)}
                >
                  +10% All
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => fetchZoneRates()}
                >
                  <RefreshCw className="h-4 w-4" />
                </Button>
              </div>
            </div>
          </CardHeader>
          <CardContent>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Zone</TableHead>
                  <TableHead>Capacité</TableHead>
                  <TableHead>Occupation</TableHead>
                  <TableHead>Tarif Horaire Actuel</TableHead>
                  <TableHead>Revenu Moyen/Jour</TableHead>
                  <TableHead>Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {zoneRates.map((zone) => (
                  <TableRow key={zone.id}>
                    <TableCell className="font-medium">{zone.name}</TableCell>
                    <TableCell>
                      <Badge variant="outline">{zone.capacity} spots</Badge>
                    </TableCell>
                    <TableCell>
                      <div className="flex items-center gap-2">
                        <div className="w-24 bg-secondary rounded-full h-2">
                          <div
                            className="bg-primary rounded-full h-2"
                            style={{ width: `${(zone.occupiedSpots / zone.capacity) * 100}%` }}
                          />
                        </div>
                        <span className="text-sm">
                          {zone.occupiedSpots}/{zone.capacity}
                        </span>
                      </div>
                    </TableCell>
                    <TableCell>
                      {editingZoneId === zone.id ? (
                        <div className="flex items-center gap-2">
                          <Input
                            type="number"
                            step="0.5"
                            min="0"
                            value={editedRate}
                            onChange={(e) => setEditedRate(parseFloat(e.target.value))}
                            className="w-24"
                          />
                          <span className="text-sm">$/h</span>
                        </div>
                      ) : (
                        <div className="flex items-center gap-2">
                          <span className="text-lg font-bold">${zone.currentRate.toFixed(2)}</span>
                          <span className="text-sm text-muted-foreground">/h</span>
                          {zone.proposedRate && zone.proposedRate > zone.currentRate && (
                            <Badge variant="outline" className="bg-green-50 text-green-700 border-green-200">
                              +${(zone.proposedRate - zone.currentRate).toFixed(2)}
                            </Badge>
                          )}
                        </div>
                      )}
                    </TableCell>
                    <TableCell>
                      <div className="flex items-center gap-2">
                        <DollarSign className="h-4 w-4 text-green-600" />
                        <span className="font-medium">${zone.averageDailyRevenue.toFixed(2)}</span>
                      </div>
                    </TableCell>
                    <TableCell>
                      {editingZoneId === zone.id ? (
                        <div className="flex gap-2">
                          <Button
                            size="sm"
                            onClick={() => handleSaveRate(zone.id)}
                          >
                            <Save className="h-4 w-4 mr-1" />
                            Save
                          </Button>
                          <Button
                            size="sm"
                            variant="outline"
                            onClick={handleCancelEdit}
                          >
                            <X className="h-4 w-4 mr-1" />
                            Cancel
                          </Button>
                        </div>
                      ) : (
                        <Button
                          size="sm"
                          variant="outline"
                          onClick={() => handleEditRate(zone)}
                        >
                          <Edit className="h-4 w-4 mr-1" />
                          Modifier
                        </Button>
                      )}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>

            {/* Rate Statistics */}
            <div className="mt-6 grid grid-cols-1 md:grid-cols-3 gap-4 pt-6 border-t">
              <div className="p-4 bg-blue-50 rounded-lg">
                <div className="text-sm font-medium text-blue-800">Tarif Moyen</div>
                <div className="text-2xl font-bold text-blue-600">
                  ${calculateAverageRate()}/h
                </div>
              </div>
              <div className="p-4 bg-green-50 rounded-lg">
                <div className="text-sm font-medium text-green-800">Revenu Journalier Estimé</div>
                <div className="text-2xl font-bold text-green-600">
                  ${calculateTotalDailyRevenue()}
                </div>
              </div>
              <div className="p-4 bg-purple-50 rounded-lg">
                <div className="text-sm font-medium text-purple-800">Revenu Mensuel Estimé</div>
                <div className="text-2xl font-bold text-purple-600">
                  ${(parseFloat(calculateTotalDailyRevenue()) * 30).toFixed(2)}
                </div>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* Financial Stats (optionnel) */}
        {statistics.financial && (
          <div className="mt-6">
            <Card>
              <CardHeader>
                <CardTitle>Financial Overview</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div className="p-4 bg-green-50 rounded-lg">
                    <div className="text-sm font-medium text-green-800">Total Revenue</div>
                    <div className="text-2xl font-bold text-green-600">
                      ${statistics.financial.totalRevenue.toFixed(2)}
                    </div>
                  </div>
                  <div className="p-4 bg-blue-50 rounded-lg">
                    <div className="text-sm font-medium text-blue-800">Today's Revenue</div>
                    <div className="text-2xl font-bold text-blue-600">
                      ${statistics.financial.todayRevenue.toFixed(2)}
                    </div>
                  </div>
                </div>
              </CardContent>
            </Card>
          </div>
        )}
      </div>
    </div>
  );
};

export default AdminDashboard;