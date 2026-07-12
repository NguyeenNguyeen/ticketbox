import * as SQLite from 'expo-sqlite';

// Tạo một biến lưu trữ instance DB để dùng chung
let db: SQLite.SQLiteDatabase | null = null;

// Hàm lấy DB đã được mở sẵn
export const getDB = () => {
  if (!db) {
    db = SQLite.openDatabaseSync('ticket_db.db');
  }
  return db;
};

export interface Ticket {
  qr_code: string;
  customer_name: string;
  status: 'ACTIVE' | 'USED' | 'REVOKED';
  is_synced: number;
  signature: string;
}

export const initDatabase = (): boolean => {
  try {
    const database = getDB();
    database.execSync(`
      CREATE TABLE IF NOT EXISTS tickets (
        qr_code TEXT PRIMARY KEY NOT NULL,
        customer_name TEXT NOT NULL,
        status TEXT NOT NULL,      
        is_synced INTEGER DEFAULT 0,
        signature TEXT NOT NULL
      );
    `);
    console.log(' Database đã sẵn sàng.');
    return true;
  } catch (error) {
    console.error(' Lỗi khởi tạo DB:', error);
    return false;
  }
};

export const insertTicket = async (ticket: Ticket): Promise<void> => {
  try {
    const database = getDB();
    await database.runAsync(
      'INSERT OR REPLACE INTO tickets (qr_code, customer_name, status, is_synced, signature) VALUES (?, ?, ?, ?, ?)',
      [ticket.qr_code, ticket.customer_name, ticket.status, ticket.is_synced, ticket.signature]
    );
  } catch (error) {
    console.error(' Lỗi insertTicket:', error);
  }
};

export const getTicketByIdAsync = async (qrCode: string): Promise<Ticket | null> => {
  try {
    const database = getDB();
    // Dùng getFirstSync thay vì getFirstAsync để đảm bảo dữ liệu trả về ngay lập tức
    // tránh các lỗi bất đồng bộ chưa kịp chuẩn bị (prepareAsync)
    const result = database.getFirstSync<Ticket>('SELECT * FROM tickets WHERE qr_code = ?', [qrCode.trim()]);
    return result || null;
  } catch (error) {
    console.error(' Lỗi getTicketById:', error);
    return null;
  }
};

export const updateTicketStatusAsync = async (qrCode: string): Promise<void> => {
  try {
    const database = getDB();
    await database.runAsync(
      'UPDATE tickets SET status = ?, is_synced = ? WHERE qr_code = ?', 
      ['USED', 0, qrCode.trim()]
    );
  } catch (error) {
    console.error(' Lỗi updateTicketStatus:', error);
  }
};