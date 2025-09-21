import React from 'react';
import { Layout, LayoutProps } from 'react-admin';
import CustomSidebar from './CustomSidebar';
import CustomAppBar from './CustomAppBar';

const CustomLayout = (props: LayoutProps) => (
  <Layout 
    {...props} 
    sidebar={CustomSidebar}
    appBar={CustomAppBar}
    sx={{
      '& .RaLayout-content': {
        backgroundColor: '#F5F6FA',
        padding: 2,
      },
    }}
  />
);

export default CustomLayout;
