import React, { useState, useEffect } from 'react';
import {
  ResponsiveContainer,
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  TooltipProps,
} from 'recharts';
import { Box, Typography } from '@mui/material';

interface DataPoint {
  name: string;
  value: number;
  users?: number;
}

const defaultData: DataPoint[] = [
  { name: 'Mon', value: 45, users: 120 },
  { name: 'Tue', value: 52, users: 132 },
  { name: 'Wed', value: 48, users: 125 },
  { name: 'Thu', value: 60, users: 148 },
  { name: 'Fri', value: 55, users: 146 },
  { name: 'Sat', value: 65, users: 158 },
  { name: 'Sun', value: 60, users: 152 },
];

// Custom tooltip component
const CustomTooltip = ({ active, payload, label }: TooltipProps<number, string>) => {
  if (active && payload && payload.length) {
    return (
      <Box
        sx={{
          backgroundColor: '#fff',
          border: '1px solid #eaedf2',
          p: 1.5,
          borderRadius: 1,
          boxShadow: '0 2px 8px rgba(0,0,0,0.1)',
        }}
      >
        <Typography variant="subtitle2" sx={{ fontWeight: 600, color: '#2C3E50', mb: 0.5 }}>
          {label}
        </Typography>
        <Box sx={{ display: 'flex', alignItems: 'center', mb: 0.5 }}>
          <Box
            sx={{
              width: 10,
              height: 10,
              backgroundColor: '#4CAF50',
              borderRadius: '50%',
              mr: 1,
            }}
          />
          <Typography variant="body2" sx={{ color: '#2C3E50' }}>
            Revenue: <strong>${payload[0].value}</strong>
          </Typography>
        </Box>
        {payload[1] && (
          <Box sx={{ display: 'flex', alignItems: 'center' }}>
            <Box
              sx={{
                width: 10,
                height: 10,
                backgroundColor: '#2196F3',
                borderRadius: '50%',
                mr: 1,
              }}
            />
            <Typography variant="body2" sx={{ color: '#2C3E50' }}>
              Users: <strong>{payload[1].value}</strong>
            </Typography>
          </Box>
        )}
      </Box>
    );
  }
  return null;
};

const RevenueTrendChart: React.FC = () => {
  const [data, setData] = useState<DataPoint[]>(defaultData);

  useEffect(() => {
    const fetchRevenueTrend = async () => {
      try {
        const response = await fetch(`${process.env.REACT_APP_API_URL}/dashboard/revenue-trend`);
        if (response.ok) {
          const apiData = await response.json();
          setData(apiData);
        }
      } catch (error) {
        console.error('Failed to fetch revenue trend:', error);
        // Keep default data on error
      }
    };

    fetchRevenueTrend();
  }, []);

  // Calculate the max value for YAxis domain
  const maxValue = Math.max(...data.map(item => item.value)) * 1.2;
  
  return (
    <ResponsiveContainer width="100%" height={300}>
      <AreaChart 
        data={data}
        margin={{ top: 5, right: 30, left: 0, bottom: 5 }}
      >
        <defs>
          <linearGradient id="colorRevenue" x1="0" y1="0" x2="0" y2="1">
            <stop offset="5%" stopColor="#4CAF50" stopOpacity={0.8}/>
            <stop offset="95%" stopColor="#4CAF50" stopOpacity={0.1}/>
          </linearGradient>
          <linearGradient id="colorUsers" x1="0" y1="0" x2="0" y2="1">
            <stop offset="5%" stopColor="#2196F3" stopOpacity={0.8}/>
            <stop offset="95%" stopColor="#2196F3" stopOpacity={0.1}/>
          </linearGradient>
        </defs>
        <CartesianGrid 
          strokeDasharray="3 3" 
          vertical={false} 
          stroke="#eaedf2" 
        />
        <XAxis 
          dataKey="name" 
          axisLine={false}
          tickLine={false}
          tick={{ fill: '#6c757d', fontSize: 12 }}
          dy={10}
        />
        <YAxis 
          axisLine={false}
          tickLine={false}
          tick={{ fill: '#6c757d', fontSize: 12 }}
          domain={[0, maxValue]}
          tickFormatter={(value) => `$${value}`}
        />
        <Tooltip content={<CustomTooltip />} />
        <Legend 
          verticalAlign="top" 
          align="right"
          iconType="circle"
          wrapperStyle={{ paddingBottom: '10px' }}
        />
        <Area
          type="monotone"
          dataKey="value"
          name="Revenue"
          stroke="#4CAF50"
          strokeWidth={2}
          fillOpacity={1}
          fill="url(#colorRevenue)"
          activeDot={{ r: 6, strokeWidth: 0 }}
        />
        <Area
          type="monotone"
          dataKey="users"
          name="Active Users"
          stroke="#2196F3"
          strokeWidth={2}
          fillOpacity={0.3}
          fill="url(#colorUsers)"
          activeDot={{ r: 6, strokeWidth: 0 }}
        />
      </AreaChart>
    </ResponsiveContainer>
  );
};

export default RevenueTrendChart;
