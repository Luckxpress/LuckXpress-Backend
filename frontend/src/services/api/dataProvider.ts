import { DataProvider, fetchUtils } from 'react-admin';
import { stringify } from 'query-string';

const apiUrl = process.env.REACT_APP_API_URL || 'http://localhost:8080/api/v1';
const httpClient = fetchUtils.fetchJson;

// Mock data for demonstration
const mockData = {
  users: [
    {
      id: 1,
      username: 'john_doe',
      email: 'john.doe@email.com',
      fullName: 'John Doe',
      status: 'active',
      kycStatus: 'verified',
      goldCoins: 1000,
      sweepCoins: 50,
      createdAt: '2024-01-15T10:30:00Z',
      state: 'CA',
    },
    {
      id: 2,
      username: 'jane_smith',
      email: 'jane.smith@email.com',
      fullName: 'Jane Smith',
      status: 'active',
      kycStatus: 'pending',
      goldCoins: 500,
      sweepCoins: 25,
      createdAt: '2024-01-18T11:15:00Z',
      state: 'NY',
    },
  ],
  payments: [
    {
      id: 1,
      userId: 1,
      amount: 100,
      type: 'deposit',
      status: 'completed',
      timestamp: '2024-01-21T10:30:00Z',
      paymentMethod: 'Credit Card',
    },
  ],
  kyc: [
    {
      id: 1,
      userId: 1,
      status: 'pending',
      documentType: 'passport',
      uploadedAt: '2024-01-20T14:30:00Z',
    },
  ],
  withdrawals: [
    {
      id: 1,
      userId: 2,
      amount: 200,
      status: 'pending',
      timestamp: '2024-01-21T11:15:00Z',
      method: 'Bank Transfer',
    },
  ],
  leads: [
    {
      id: 1,
      name: 'Alice Johnson',
      email: 'alice.johnson@email.com',
      phone: '+1-555-0123',
      status: 'new',
      createdAt: '2024-01-21T09:30:00Z',
    },
    {
      id: 2,
      name: 'Bob Wilson',
      email: 'bob.wilson@email.com',
      phone: '+1-555-0124',
      status: 'contacted',
      createdAt: '2024-01-20T14:15:00Z',
    },
  ],
};

const dataProvider: DataProvider = {
  getList: (resource, params) => {
    const { page, perPage } = params.pagination;
    const { field, order } = params.sort;
    const query = {
      sort: JSON.stringify([field, order]),
      range: JSON.stringify([(page - 1) * perPage, page * perPage - 1]),
      filter: JSON.stringify(params.filter),
    };

    // For demo purposes, return mock data
    const data = mockData[resource as keyof typeof mockData] || [];
    const total = data.length;
    
    return Promise.resolve({
      data: data.slice((page - 1) * perPage, page * perPage),
      total,
    });
  },

  getOne: (resource, params) => {
    const data = mockData[resource as keyof typeof mockData] || [];
    const item = data.find((item: any) => item.id == params.id);
    
    return item 
      ? Promise.resolve({ data: item })
      : Promise.reject(new Error(`Resource ${resource} with id ${params.id} not found`));
  },

  getMany: (resource, params) => {
    const data = mockData[resource as keyof typeof mockData] || [];
    const items = data.filter((item: any) => params.ids.includes(item.id));
    
    return Promise.resolve({ data: items });
  },

  getManyReference: (resource, params) => {
    const { page, perPage } = params.pagination;
    const { field, order } = params.sort;
    const query = {
      sort: JSON.stringify([field, order]),
      range: JSON.stringify([(page - 1) * perPage, page * perPage - 1]),
      filter: JSON.stringify({
        ...params.filter,
        [params.target]: params.id,
      }),
    };

    const data = mockData[resource as keyof typeof mockData] || [];
    const filteredData = data.filter((item: any) => item[params.target] == params.id);
    
    return Promise.resolve({
      data: filteredData.slice((page - 1) * perPage, page * perPage),
      total: filteredData.length,
    });
  },

  update: (resource, params) => {
    const data = mockData[resource as keyof typeof mockData] || [];
    const index = data.findIndex((item: any) => item.id == params.id);
    
    if (index === -1) {
      return Promise.reject(new Error(`Resource ${resource} with id ${params.id} not found`));
    }
    
    const updatedItem = { ...data[index], ...params.data };
    (data as any)[index] = updatedItem;
    
    return Promise.resolve({ data: updatedItem });
  },

  updateMany: (resource, params) => {
    const data = mockData[resource as keyof typeof mockData] || [];
    const updatedIds: any[] = [];
    
    params.ids.forEach((id) => {
      const index = data.findIndex((item: any) => item.id == id);
      if (index !== -1) {
        (data as any)[index] = { ...data[index], ...params.data };
        updatedIds.push(id);
      }
    });
    
    return Promise.resolve({ data: updatedIds });
  },

  create: (resource, params) => {
    const data = mockData[resource as keyof typeof mockData] || [];
    const newItem = {
      id: Math.max(...data.map((item: any) => item.id), 0) + 1,
      ...params.data,
    };
    
    (data as any).push(newItem);
    
    return Promise.resolve({ data: newItem });
  },

  delete: (resource, params) => {
    const data = mockData[resource as keyof typeof mockData] || [];
    const index = data.findIndex((item: any) => item.id == params.id);
    
    if (index === -1) {
      return Promise.reject(new Error(`Resource ${resource} with id ${params.id} not found`));
    }
    
    const deletedItem = data[index];
    (data as any).splice(index, 1);
    
    return Promise.resolve({ data: deletedItem });
  },

  deleteMany: (resource, params) => {
    const data = mockData[resource as keyof typeof mockData] || [];
    const deletedIds: any[] = [];
    
    params.ids.forEach((id) => {
      const index = data.findIndex((item: any) => item.id == id);
      if (index !== -1) {
        (data as any).splice(index, 1);
        deletedIds.push(id);
      }
    });
    
    return Promise.resolve({ data: deletedIds });
  },
};

export default dataProvider;
