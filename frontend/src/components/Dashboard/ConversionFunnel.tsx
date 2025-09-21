import React, { useState, useEffect } from 'react';
import { Box, Typography, LinearProgress, Tooltip, Paper } from '@mui/material';
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined';

interface FunnelStage {
  label: string;
  value: number;
  percentage: number;
  color: string;
  icon?: React.ReactNode;
  description?: string;
}

const defaultFunnelData: FunnelStage[] = [
  { 
    label: 'Visitors', 
    value: 45291, 
    percentage: 100, 
    color: '#4CAF50',
    description: 'Total unique visitors to the platform'
  },
  { 
    label: 'Signups', 
    value: 12847, 
    percentage: 28.4, 
    color: '#2196F3',
    description: 'Users who created an account'
  },
  { 
    label: 'First Deposit', 
    value: 3921, 
    percentage: 8.7, 
    color: '#FF9800',
    description: 'Users who made their first deposit'
  },
  { 
    label: 'Active Players', 
    value: 2847, 
    percentage: 6.3, 
    color: '#9C27B0',
    description: 'Users who placed at least one bet'
  },
];

const ConversionFunnel: React.FC = () => {
  const [funnelData, setFunnelData] = useState<FunnelStage[]>(defaultFunnelData);
  const [hoveredStage, setHoveredStage] = useState<number | null>(null);

  useEffect(() => {
    const fetchConversionFunnel = async () => {
      try {
        const response = await fetch(`${process.env.REACT_APP_API_URL}/dashboard/conversion-funnel`);
        if (response.ok) {
          const apiData = await response.json();
          // Add colors and descriptions to the API data
          const dataWithColors = apiData.map((item: any, index: number) => ({
            ...item,
            color: defaultFunnelData[index]?.color || '#4CAF50',
            description: defaultFunnelData[index]?.description || ''
          }));
          setFunnelData(dataWithColors);
        }
      } catch (error) {
        console.error('Failed to fetch conversion funnel:', error);
        // Keep default data on error
      }
    };

    fetchConversionFunnel();
  }, []);

  // Calculate conversion rates between stages
  const getConversionRate = (currentIndex: number) => {
    if (currentIndex === 0) return 100;
    const currentValue = funnelData[currentIndex].value;
    const previousValue = funnelData[currentIndex - 1].value;
    return ((currentValue / previousValue) * 100).toFixed(1);
  };

  return (
    <Box sx={{ p: 0 }}>
      {funnelData.map((stage, index) => {
        const conversionRate = getConversionRate(index);
        const isLast = index === funnelData.length - 1;
        
        return (
          <Box 
            key={stage.label} 
            sx={{ 
              mb: isLast ? 0 : 3,
              position: 'relative',
              '&:hover': {
                '& .stage-info': {
                  opacity: 1,
                  visibility: 'visible',
                  transform: 'translateY(0)',
                }
              }
            }}
            onMouseEnter={() => setHoveredStage(index)}
            onMouseLeave={() => setHoveredStage(null)}
          >
            <Box 
              display="flex" 
              justifyContent="space-between" 
              alignItems="center" 
              mb={1}
            >
              <Box display="flex" alignItems="center">
                <Box 
                  sx={{ 
                    width: 12, 
                    height: 12, 
                    borderRadius: '50%', 
                    backgroundColor: stage.color,
                    mr: 1.5
                  }} 
                />
                <Typography 
                  variant="body2" 
                  sx={{ 
                    fontWeight: 600, 
                    color: '#2C3E50',
                    fontSize: '0.875rem'
                  }}
                >
                  {stage.label}
                </Typography>
                <Tooltip title={stage.description || ''}>
                  <InfoOutlinedIcon 
                    sx={{ 
                      ml: 0.5, 
                      fontSize: 16, 
                      color: 'rgba(0,0,0,0.3)',
                      cursor: 'help'
                    }} 
                  />
                </Tooltip>
              </Box>
              <Box display="flex" alignItems="center">
                <Typography 
                  variant="body2" 
                  sx={{ 
                    fontWeight: 700, 
                    color: '#2C3E50',
                    mr: 1
                  }}
                >
                  {stage.value.toLocaleString()}
                </Typography>
                <Typography 
                  variant="caption" 
                  sx={{ 
                    color: 'text.secondary',
                    backgroundColor: 'rgba(0,0,0,0.05)',
                    px: 1,
                    py: 0.5,
                    borderRadius: 1,
                    fontSize: '0.7rem'
                  }}
                >
                  {stage.percentage}%
                </Typography>
              </Box>
            </Box>
            
            <Box sx={{ position: 'relative' }}>
              <LinearProgress
                variant="determinate"
                value={stage.percentage}
                sx={{
                  height: 12,
                  borderRadius: 6,
                  backgroundColor: 'rgba(0,0,0,0.05)',
                  '& .MuiLinearProgress-bar': {
                    backgroundColor: stage.color,
                    borderRadius: 6,
                  },
                }}
              />
              
              {/* Stage conversion info tooltip */}
              {index > 0 && hoveredStage === index && (
                <Paper 
                  elevation={3}
                  className="stage-info"
                  sx={{
                    position: 'absolute',
                    top: -60,
                    left: '50%',
                    transform: 'translateX(-50%) translateY(10px)',
                    p: 1.5,
                    borderRadius: 1,
                    backgroundColor: 'white',
                    zIndex: 10,
                    opacity: 0,
                    visibility: 'hidden',
                    transition: 'all 0.2s ease',
                    width: 180,
                    textAlign: 'center',
                    '&:after': {
                      content: '""',
                      position: 'absolute',
                      bottom: -8,
                      left: '50%',
                      transform: 'translateX(-50%)',
                      width: 0,
                      height: 0,
                      borderLeft: '8px solid transparent',
                      borderRight: '8px solid transparent',
                      borderTop: '8px solid white',
                    }
                  }}
                >
                  <Typography variant="caption" sx={{ fontWeight: 600, color: '#2C3E50', display: 'block' }}>
                    Stage Conversion Rate
                  </Typography>
                  <Typography variant="h6" sx={{ fontWeight: 700, color: stage.color }}>
                    {conversionRate}%
                  </Typography>
                  <Typography variant="caption" sx={{ color: 'text.secondary', display: 'block' }}>
                    from {funnelData[index-1].label}
                  </Typography>
                </Paper>
              )}
            </Box>
            
            {!isLast && (
              <Box 
                sx={{ 
                  height: 24, 
                  width: 2, 
                  backgroundColor: 'rgba(0,0,0,0.05)', 
                  ml: '6px', 
                  mt: 0.5, 
                  mb: 0.5 
                }} 
              />
            )}
          </Box>
        );
      })}
      
      <Box sx={{ mt: 3, pt: 2, borderTop: '1px dashed rgba(0,0,0,0.1)', display: 'flex', justifyContent: 'space-between' }}>
        <Typography variant="caption" sx={{ color: 'text.secondary' }}>
          Total Conversion Rate
        </Typography>
        <Typography variant="caption" sx={{ fontWeight: 700, color: '#4CAF50' }}>
          {funnelData.length > 0 ? 
            ((funnelData[funnelData.length-1].value / funnelData[0].value) * 100).toFixed(2) : 0}%
        </Typography>
      </Box>
    </Box>
  );
};

export default ConversionFunnel;
