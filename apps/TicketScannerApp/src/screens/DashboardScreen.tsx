import React, { useState, useEffect } from 'react';
import { StyleSheet, Text, View, TouchableOpacity, Alert, ActivityIndicator } from 'react-native';
import * as SQLite from 'expo-sqlite';
import NetInfo from '@react-native-community/netinfo';
import { apiService, SyncTicketPayload } from '../services/apiService';
import { getDB } from '../database/database'; 
import { seedDatabase } from '../database/seedDatabase'; 

const db = getDB();

export default function DashboardScreen({ onNavigateToScanner, isFocused }: any) {
  const [stats, setStats] = useState({ total: 0, scanned: 0, unsynced: 0 });
  const [loading, setLoading] = useState<boolean>(false);
  const [dataLoading, setDataLoading] = useState<boolean>(true);

  const loadStatistics = async () => {
    try {
      const totalRows = await db.getAllAsync('SELECT qr_code FROM tickets');
      const scannedRows = await db.getAllAsync('SELECT qr_code FROM tickets WHERE status = "USED"');
      const unsyncedRows = await db.getAllAsync('SELECT qr_code FROM tickets WHERE status = "USED" AND is_synced = 0');

      setStats({
        total: totalRows.length,
        scanned: scannedRows.length,
        unsynced: unsyncedRows.length
      });
    } catch (error) {
      console.error("Lỗi load thống kê:", error);
    } finally {
      setDataLoading(false);
    }
  };

  const handleSyncData = async () => {
    if (stats.unsynced === 0) {
      Alert.alert("Thông báo", "Không có vé nào cần đồng bộ.");
      return;
    }

    setLoading(true);
    try {
      const unsynced = await db.getAllAsync<{qr_code: string}>('SELECT qr_code FROM tickets WHERE status = "USED" AND is_synced = 0');
      const payload: SyncTicketPayload[] = unsynced.map(t => ({ 
        ticketId: t.qr_code, 
        status: 'USED', 
        scannedAt: new Date().toISOString() 
      }));
      
      const isSuccess = await apiService.syncTicketsToServer(payload, "TOKEN_DEMO");

      if (isSuccess) {
        await db.runAsync('UPDATE tickets SET is_synced = 1 WHERE status = "USED" AND is_synced = 0');
        await loadStatistics();
        Alert.alert("Thành công ", `Đã đồng bộ ${payload.length} vé!`);
      } else {
        Alert.alert(" Lỗi Sync", "Không kết nối được server!");
      }
    } catch (e) { 
      console.error(e);
    } finally { 
      setLoading(false); 
    }
  };

  const handleResetData = async () => {
    try {
      await db.runAsync('DELETE FROM tickets'); 
      await seedDatabase(); // Nạp lại 30 vé với tên mới
      await loadStatistics();
      Alert.alert(" Đã reset!", "Dữ liệu đã được cập nhật tên mới hoàn toàn.");
    } catch (e) { 
      console.error(e);
      Alert.alert(" Lỗi", "Không thể reset database"); 
    }
  };

  useEffect(() => {
    if (isFocused) {
      loadStatistics();
    }
  }, [isFocused]);

  return (
    <View style={styles.container}>
      <Text style={styles.title}>QUẢN LÝ SOÁT VÉ</Text>
      <View style={styles.statsContainer}>
        <View style={[styles.card, { backgroundColor: '#2196F3' }]}><Text style={styles.cardValue}>{stats.total}</Text><Text style={styles.cardLabel}>Tổng</Text></View>
        <View style={[styles.card, { backgroundColor: '#4CAF50' }]}><Text style={styles.cardValue}>{stats.scanned}</Text><Text style={styles.cardLabel}>Đã Quét</Text></View>
        <View style={[styles.card, { backgroundColor: '#FF9800' }]}><Text style={styles.cardValue}>{stats.unsynced}</Text><Text style={styles.cardLabel}>Chờ Sync</Text></View>
      </View>
      
      <TouchableOpacity style={styles.button} onPress={onNavigateToScanner}>
        <Text style={styles.buttonText}>📸 QUÉT VÉ</Text>
      </TouchableOpacity>
      
      <TouchableOpacity style={[styles.button, { backgroundColor: '#333' }]} onPress={handleSyncData}>
        {loading ? <ActivityIndicator color="#fff" /> : <Text style={[styles.buttonText, { color: '#FF9800' }]}>☁️ ĐỒNG BỘ</Text>}
      </TouchableOpacity>

      <TouchableOpacity style={[styles.button, { backgroundColor: '#d32f2f', marginTop: 30 }]} onPress={handleResetData}>
        <Text style={styles.buttonText}>🔄 RESET DATABASE</Text>
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#121212', alignItems: 'center', justifyContent: 'center', padding: 20 },
  title: { fontSize: 20, fontWeight: 'bold', color: '#fff', marginBottom: 40 },
  statsContainer: { flexDirection: 'row', justifyContent: 'space-between', width: '100%', marginBottom: 50 },
  card: { width: '30%', padding: 20, borderRadius: 12, alignItems: 'center' },
  cardValue: { fontSize: 22, fontWeight: 'bold', color: '#fff' },
  cardLabel: { fontSize: 12, color: '#fff' },
  button: { width: '85%', padding: 15, borderRadius: 25, alignItems: 'center', marginVertical: 10, backgroundColor: '#00ff00' },
  buttonText: { fontSize: 16, fontWeight: 'bold' }
});

