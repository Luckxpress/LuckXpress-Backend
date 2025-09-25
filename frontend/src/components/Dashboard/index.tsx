import React, { useEffect, useState } from 'react';
import axios from 'axios';

const Dashboard = () => {
  const [metrics, setMetrics] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchMetrics = async () => {
      try {
        const response = await axios.get('http://localhost:8080/api/v1/dashboard/metrics');
        setMetrics(response.data);
        setLoading(false);
      } catch (err) {
        setError('Failed to fetch metrics');
        setLoading(false);
      }
    };

    fetchMetrics();
    const interval = setInterval(fetchMetrics, 30000); // Refresh every 30 seconds

    return () => clearInterval(interval);
  }, []);

  // Rest of component...
};
