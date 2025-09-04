import express, { Request, Response } from 'express';

const app = express();

app.get('/', (req: Request, res: Response) => {
  res.json({ message: 'Welcome to LuckXpress Backend API' });
});

export default app;