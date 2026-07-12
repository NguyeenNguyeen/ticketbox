import React, { useEffect, useState } from 'react';
import { StyleSheet, Text, View, FlatList, ActivityIndicator } from 'react-native';
import { getDB } from '../database/database';

const db = getDB();

interface TicketHistory {
  qr_code: string;
  customer_name: string;
  status: string;
}

export default function HistoryScreen({ isFocused }: any) {
  const [history, setHistory] = useState<TicketHistory[]>([]);
  const [loading, setLoading] = useState<boolean>(true);

  const loadHistory = async () => {
    try {
      setLoading(true);
      // Lấy danh sách vé đã quét, mới nhất lên đầu
      const result = await db.getAllAsync<TicketHistory>(
        'SELECT * FROM tickets WHERE status = "USED" ORDER BY rowid DESC'
      );
      setHistory(result);
    } catch (error) {
      console.error("Lỗi tải lịch sử:", error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (isFocused) {
      loadHistory();
    }
  }, [isFocused]);

  const renderItem = ({ item }: { item: TicketHistory }) => (
    <View style={styles.item}>
      <View>
        <Text style={styles.name}>{item.customer_name}</Text>
        <Text style={styles.code}>{item.qr_code}</Text>
      </View>
      <View style={styles.badge}><Text style={styles.badgeText}>Đã quét</Text></View>
    </View>
  );

  if (loading) return <ActivityIndicator size="large" color="#00ff00" style={{ marginTop: 50 }} />;

  return (
    <View style={styles.container}>
      <Text style={styles.title}>LỊCH SỬ SOÁT VÉ</Text>
      {history.length === 0 ? (
        <Text style={styles.emptyText}>Chưa có vé nào được quét.</Text>
      ) : (
        <FlatList
          data={history}
          keyExtractor={(item) => item.qr_code}
          renderItem={renderItem}
          contentContainerStyle={{ paddingBottom: 20 }}
        />
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#121212', padding: 20 },
  title: { fontSize: 20, fontWeight: 'bold', color: '#fff', marginBottom: 20, textAlign: 'center' },
  item: { backgroundColor: '#1e1e1e', padding: 15, borderRadius: 10, marginBottom: 10, flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  name: { color: '#fff', fontSize: 16, fontWeight: 'bold' },
  code: { color: '#888', fontSize: 12 },
  badge: { backgroundColor: '#4CAF50', paddingHorizontal: 8, paddingVertical: 4, borderRadius: 5 },
  badgeText: { color: '#fff', fontSize: 10 },
  emptyText: { color: '#888', textAlign: 'center', marginTop: 50 }
});


// import React, { useState, useEffect } from 'react';
// import { StyleSheet, Text, View, FlatList, ActivityIndicator, TouchableOpacity } from 'react-native';
// import * as SQLite from 'expo-sqlite';

// const db = SQLite.openDatabaseSync('ticket_db.db');

// interface Ticket {
//   qr_code: string;
//   customer_name: string;
//   status: number;
//   is_synced: number;
// }

// interface HistoryScreenProps {
//   isFocused: boolean;
// }

// export default function HistoryScreen({ isFocused }: HistoryScreenProps) {
//   const [tickets, setTickets] = useState<Ticket[]>([]);
//   const [loading, setLoading] = useState<boolean>(true);

//   const fetchAllTickets = async () => {
//     try {
//       // Lấy toàn bộ danh sách vé trong máy để hiển thị
//       const rows = await db.getAllAsync<Ticket>('SELECT * FROM tickets');
//       setTickets(rows);
//     } catch (error) {
//       console.error("Lỗi lấy danh sách lịch sử:", error);
//     } finally {
//       setLoading(false);
//     }
//   };

//   useEffect(() => {
//     if (isFocused) {
//       setLoading(true);
//       fetchAllTickets();
//     }
//   }, [isFocused]);

//   const renderTicketItem = ({ item }: { item: Ticket }) => (
//     <View style={styles.ticketCard}>
//       <View style={{ flex: 1 }}>
//         <Text style={styles.customerName}>{item.customer_name}</Text>
//         <Text style={styles.ticketId}>Mã: {item.qr_code}</Text>
//       </View>
      
//       <View style={styles.statusContainer}>
//         {/* Trạng thái Quét */}
//         <Text style={[styles.badge, item.status === 1 ? styles.badgeScanned : styles.badgeUnscanned]}>
//           {item.status === 1 ? ' Đã Quét' : ' Chưa Quét'}
//         </Text>
        
//         {/* Trạng thái Sync (Chỉ hiện nếu vé đã quét) */}
//         {item.status === 1 && (
//           <Text style={[styles.badge, item.is_synced === 1 ? styles.badgeSynced : styles.badgeUnsynced]}>
//             {item.is_synced === 1 ? ' Đã Sync' : ' Local'}
//           </Text>
//         )}
//       </View>
//     </View>
//   );

//   if (loading) {
//     return (
//       <View style={styles.center}>
//         <ActivityIndicator size="large" color="#00ff00" />
//       </View>
//     );
//   }

//   return (
//     <View style={styles.container}>
//       <Text style={styles.title}>DANH SÁCH VÉ HỆ THỐNG</Text>
//       <FlatList
//         data={tickets}
//         keyExtractor={(item) => item.qr_code}
//         renderItem={renderTicketItem}
//         contentContainerStyle={{ width: '100%', paddingBottom: 20 }}
//       />
//     </View>
//   );
// }

// const styles = StyleSheet.create({
//   container: {
//     flex: 1,
//     backgroundColor: '#121212',
//     width: '100%',
//     paddingHorizontal: 15,
//     paddingTop: 20,
//   },
//   center: {
//     flex: 1,
//     backgroundColor: '#121212',
//     justifyContent: 'center',
//     alignItems: 'center',
//   },
//   title: {
//     fontSize: 18,
//     fontWeight: 'bold',
//     color: '#fff',
//     marginBottom: 15,
//     textAlign: 'center',
//     letterSpacing: 1,
//   },
//   ticketCard: {
//     backgroundColor: '#1e1e1e',
//     padding: 15,
//     borderRadius: 10,
//     marginVertical: 6,
//     flexDirection: 'row',
//     alignItems: 'center',
//     borderWidth: 1,
//     borderColor: '#333',
//   },
//   customerName: {
//     color: '#fff',
//     fontSize: 16,
//     fontWeight: 'bold',
//   },
//   ticketId: {
//     color: '#aaa',
//     fontSize: 13,
//     marginTop: 4,
//   },
//   statusContainer: {
//     alignItems: 'flex-end',
//     gap: 5,
//   },
//   badge: {
//     fontSize: 11,
//     fontWeight: 'bold',
//     paddingHorizontal: 8,
//     paddingVertical: 4,
//     borderRadius: 5,
//     overflow: 'hidden',
//   },
//   badgeScanned: { backgroundColor: '#2e7d32', color: '#fff' },
//   badgeUnscanned: { backgroundColor: '#37474f', color: '#b0bec5' },
//   badgeSynced: { backgroundColor: '#1565c0', color: '#fff' },
//   badgeUnsynced: { backgroundColor: '#ef6c00', color: '#fff' },
// });