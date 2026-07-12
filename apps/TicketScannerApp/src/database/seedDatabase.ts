import { getDB, insertTicket, Ticket } from './database';
// Thêm 'as Ticket[]' để TypeScript hiểu đây là mảng các đối tượng Ticket
import data from '../../admin-tools/tickets_data.json'; 
const ticketsData = data as Ticket[];

export const isDatabaseEmpty = async (): Promise<boolean> => {
  try {
    const db = getDB();
    const result = await db.getFirstAsync<{ count: number }>('SELECT COUNT(*) as count FROM tickets');
    return result ? result.count === 0 : true;
  } catch (error) {
    return true;
  }
};

export const seedDatabase = async (): Promise<void> => {
  try {
    console.log("Đang nạp dữ liệu từ file JSON...");
    
    for (const ticket of ticketsData) {
      // Ép kiểu (type assertion) để TypeScript hiểu đối tượng mới vẫn là 1 Ticket
      const cleanTicket: Ticket = {
        ...ticket,
        status: 'ACTIVE',
        is_synced: 0
      };
      
      await insertTicket(cleanTicket);
    }
    console.log(" Đã nạp thành công 30 vé vào DB.");
  } catch (error) {
    console.error(" Lỗi khi seeding:", error);
  }
};