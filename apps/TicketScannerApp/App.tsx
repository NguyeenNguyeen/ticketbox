import React, { useEffect, useState } from 'react';
import { StyleSheet, Text, View, Button, TouchableOpacity, ActivityIndicator } from 'react-native';
import { useCameraPermissions } from 'expo-camera';
import { StatusBar } from 'expo-status-bar';
import { initDatabase } from './src/database/database';
import { seedDatabase, isDatabaseEmpty } from './src/database/seedDatabase'; 
import ScannerScreen from './src/screens/ScannerScreen';
import DashboardScreen from './src/screens/DashboardScreen';
import HistoryScreen from './src/screens/HistoryScreen';

export default function App() {
  const [permission, requestPermission] = useCameraPermissions();
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [currentScreen, setCurrentScreen] = useState<'dashboard' | 'scanner' | 'history'>('dashboard');

  useEffect(() => {
    const setupApp = async () => {
      try {
        const isReady = initDatabase();
        if (isReady) {
          const empty = await isDatabaseEmpty();
          if (empty) {
            console.log("Phát hiện Database trống, nạp 30 vé...");
            await seedDatabase();
          } else {
            console.log("Database đã có sẵn dữ liệu.");
          }
        }
      } catch (error) {
        console.error("Lỗi khởi tạo:", error);
      } finally {
        setIsLoading(false);
      }
    };
    setupApp();
  }, []);

  if (!permission) return <View style={styles.container}><ActivityIndicator size="large" color="#00ff00" /></View>;
  if (!permission.granted) return (
      <View style={styles.container}>
        <Text style={styles.text}>App cần quyền camera!</Text>
        <Button onPress={requestPermission} title="Cấp quyền" />
      </View>
  );

  if (isLoading) return (
    <View style={styles.container}>
      <ActivityIndicator size="large" color="#00ff00" style={{marginTop: 100}} />
      <Text style={styles.text}>Đang chuẩn bị hệ thống soát vé...</Text>
    </View>
  );

  return (
    <View style={styles.container}>
      <View style={styles.mainContent}>
        {currentScreen === 'dashboard' && (
          <DashboardScreen onNavigateToScanner={() => setCurrentScreen('scanner')} isFocused={true} />
        )}
        {currentScreen === 'history' && <HistoryScreen isFocused={true} />}
        
        {/* Truyền hàm onNavigateToDashboard xuống ScannerScreen */}
        {currentScreen === 'scanner' && (
          <ScannerScreen onNavigateToDashboard={() => setCurrentScreen('dashboard')} />
        )}
      </View>
      
      {currentScreen !== 'scanner' && (
        <View style={styles.tabBar}>
          <TouchableOpacity style={styles.tabButton} onPress={() => setCurrentScreen('dashboard')}>
            <Text style={[styles.tabText, currentScreen === 'dashboard' && styles.activeTabText]}>Dashboard</Text>
          </TouchableOpacity>
          <TouchableOpacity style={styles.tabButton} onPress={() => setCurrentScreen('history')}>
            <Text style={[styles.tabText, currentScreen === 'history' && styles.activeTabText]}>Lịch Sử</Text>
          </TouchableOpacity>
        </View>
      )}
      <StatusBar style="light" />
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#121212', justifyContent: 'center' },
  mainContent: { flex: 1, width: '100%' },
  text: { color: '#fff', textAlign: 'center', marginTop: 20 },
  tabBar: { flexDirection: 'row', height: 65, backgroundColor: '#1e1e1e', borderTopWidth: 1, borderColor: '#333', paddingBottom: 5 },
  tabButton: { flex: 1, justifyContent: 'center', alignItems: 'center' },
  tabText: { color: '#888', fontSize: 14, fontWeight: '500' },
  activeTabText: { color: '#00ff00', fontWeight: 'bold' }
});


// import React, { useEffect, useState } from 'react';
// import { StyleSheet, Text, View, Button, TouchableOpacity, ActivityIndicator } from 'react-native';
// import { useCameraPermissions } from 'expo-camera';
// import { StatusBar } from 'expo-status-bar';
// import { initDatabase } from './src/database/database';
// import { seedDatabase, isDatabaseEmpty } from './src/database/seedDatabase'; 
// import ScannerScreen from './src/screens/ScannerScreen';
// import DashboardScreen from './src/screens/DashboardScreen';
// import HistoryScreen from './src/screens/HistoryScreen';

// export default function App() {
//   const [permission, requestPermission] = useCameraPermissions();
//   const [isLoading, setIsLoading] = useState<boolean>(true); // Quản lý loading
//   const [currentScreen, setCurrentScreen] = useState<'dashboard' | 'scanner' | 'history'>('dashboard');

//   useEffect(() => {
//     const setupApp = async () => {
//       try {
//         const isReady = initDatabase();
//         if (isReady) {
//           const empty = await isDatabaseEmpty();
//           if (empty) {
//             console.log("Phát hiện Database trống, nạp 30 vé...");
//             await seedDatabase();
//           } else {
//             console.log("Database đã có sẵn dữ liệu.");
//           }
//         }
//       } catch (error) {
//         console.error("Lỗi khởi tạo:", error);
//       } finally {
//         setIsLoading(false); // Xong hết mới cho hiện màn hình
//       }
//     };
//     setupApp();
//   }, []);

//   if (!permission) return <View style={styles.container}><ActivityIndicator size="large" color="#00ff00" /></View>;
//   if (!permission.granted) return (
//       <View style={styles.container}>
//         <Text style={styles.text}>App cần quyền camera!</Text>
//         <Button onPress={requestPermission} title="Cấp quyền" />
//       </View>
//   );

//   // Màn hình chờ khi đang load DB
//   if (isLoading) return (
//     <View style={styles.container}>
//       <ActivityIndicator size="large" color="#00ff00" style={{marginTop: 100}} />
//       <Text style={styles.text}>Đang chuẩn bị hệ thống soát vé...</Text>
//     </View>
//   );

//   return (
//     <View style={styles.container}>
//       <View style={styles.mainContent}>
//         {currentScreen === 'dashboard' && <DashboardScreen onNavigateToScanner={() => setCurrentScreen('scanner')} isFocused={true} />}
//         {currentScreen === 'history' && <HistoryScreen isFocused={true} />}
//         {currentScreen === 'scanner' && <ScannerScreen />}
//       </View>
      
//       {/* Tab bar chỉ hiện khi không ở màn hình Scanner (để tránh che khuất camera) */}
//       {currentScreen !== 'scanner' && (
//         <View style={styles.tabBar}>
//           <TouchableOpacity style={styles.tabButton} onPress={() => setCurrentScreen('dashboard')}><Text style={styles.tabText}>Dashboard</Text></TouchableOpacity>
//           <TouchableOpacity style={styles.tabButton} onPress={() => setCurrentScreen('history')}><Text style={styles.tabText}>Lịch Sử</Text></TouchableOpacity>
//         </View>
//       )}
//       <StatusBar style="light" />
//     </View>
//   );
// }

// const styles = StyleSheet.create({
//   container: { flex: 1, backgroundColor: '#121212', justifyContent: 'center' },
//   mainContent: { flex: 1, width: '100%' },
//   text: { color: '#fff', textAlign: 'center', marginTop: 20 },
//   tabBar: { flexDirection: 'row', height: 65, backgroundColor: '#1e1e1e', borderTopWidth: 1, borderColor: '#333', paddingBottom: 5 },
//   tabButton: { flex: 1, justifyContent: 'center', alignItems: 'center' },
//   tabText: { color: '#888', fontSize: 14, fontWeight: '500' },
// });


