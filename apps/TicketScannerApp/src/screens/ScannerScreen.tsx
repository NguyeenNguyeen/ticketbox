import React, { useState, useRef } from 'react'; // 1. Import thêm useRef
import { StyleSheet, Text, View, TouchableOpacity, Alert } from 'react-native';
import { CameraView, BarcodeScanningResult } from 'expo-camera';
import { getTicketByIdAsync, updateTicketStatusAsync } from '../database/database';
import { verifyTicketSignature } from '../utils/cryptoUtils';

export default function ScannerScreen({ onNavigateToDashboard }: any) {
  const [scanned, setScanned] = useState<boolean>(false);
  const [ticketData, setTicketData] = useState<string>('Chưa có dữ liệu vé');
  
  // 2. Tạo một chốt chặn dùng ref (hoạt động tức thì)
  const isLock = useRef(false); 

  const handleBarcodeScanned = async (result: BarcodeScanningResult) => {
    if (isLock.current || scanned) return;
    
    isLock.current = true;
    setScanned(true); 
    
    const data = result.data;
    setTicketData(data);

    try {
      const parsedData = JSON.parse(data);
      const isSignatureValid = verifyTicketSignature(parsedData.qr_code, parsedData.signature);
      
      if (!isSignatureValid) {
        Alert.alert(" CẢNH BÁO", "Vé giả hoặc chữ ký không hợp lệ!", [
          { text: "OK", onPress: () => { isLock.current = false; setScanned(false); } }
        ]);
        return;
      }

      const ticket = await getTicketByIdAsync(parsedData.qr_code);

      if (!ticket) {
        Alert.alert(" VÉ KHÔNG TỒN TẠI", "Vé này không có trong hệ thống.", [
          { text: "OK", onPress: () => { isLock.current = false; setScanned(false); } }
        ]);
      } else if (ticket.status === 'USED') {
        Alert.alert("⚠️ VÉ ĐÃ DÙNG", `Vé của ${ticket.customer_name} đã được sử dụng.`, [
          { text: "OK", onPress: () => { isLock.current = false; setScanned(false); } }
        ]);
      } else {
        await updateTicketStatusAsync(parsedData.qr_code);
        Alert.alert(" SOÁT VÉ THÀNH CÔNG", `Chào mừng: ${ticket.customer_name}`, [
          { text: "OK", onPress: () => { /* Giữ scanned=true để hiện 2 nút */ } }
        ]);
      }
    } catch (e) {
      Alert.alert(" LỖI", "Mã QR không đúng định dạng!", [
        { text: "OK", onPress: () => { isLock.current = false; setScanned(false); } }
      ]);
    }
  };

  // return (
  //   <View style={styles.container}>
  //     <Text style={styles.title}>OFFLINE TICKET SCANNER</Text>
      
  //     <View style={styles.cameraContainer}>
  //       <CameraView
  //         style={StyleSheet.absoluteFillObject}
  //         facing="back"
  //         onBarcodeScanned={scanned ? undefined : handleBarcodeScanned}
  //         barcodeScannerSettings={{ barcodeTypes: ["qr"] }}
  //       />
  //       <View style={styles.overlayScanner}></View>
  //     </View>

  //     <View style={styles.resultContainer}>
  //       <Text style={styles.resultLabel}>Mã quét gần nhất:</Text>
  //       <Text style={styles.resultText}>{ticketData}</Text>
        
  //       {scanned && (
  //         <View style={styles.buttonRow}>
  //           <TouchableOpacity 
  //             style={[styles.button, { backgroundColor: '#555', width: '45%' }]} 
  //             onPress={() => {
  //               isLock.current = false; // Mở khóa chốt
  //               setScanned(false);
  //             }}
  //           >
  //             <Text style={styles.buttonText}>Tiếp tục</Text>
  //           </TouchableOpacity>

  //           <TouchableOpacity 
  //             style={[styles.button, { backgroundColor: '#FF9800', width: '45%' }]} 
  //             onPress={() => onNavigateToDashboard?.()}
  //           >
  //             <Text style={styles.buttonText}>Trang chủ</Text>
  //           </TouchableOpacity>
  //         </View>
  //       )}
  //     </View>
  //   </View>
  // );
  return (
    <View style={styles.container}>
      <Text style={styles.title}>OFFLINE TICKET SCANNER</Text>
      
      <View style={styles.cameraContainer}>
        <CameraView
          style={StyleSheet.absoluteFillObject}
          facing="back"
          onBarcodeScanned={scanned ? undefined : handleBarcodeScanned}
          barcodeScannerSettings={{ barcodeTypes: ["qr"] }}
        />
        <View style={styles.overlayScanner}></View>
      </View>
  
      <View style={styles.resultContainer}>
        <Text style={styles.resultLabel}>Mã quét gần nhất:</Text>
        <Text style={styles.resultText}>{ticketData}</Text>
        
        {/* Khối này chứa cả nút Tiếp tục (chỉ hiện khi quét xong) và nút Trang chủ (luôn hiện) */}
        <View style={styles.buttonRow}>
          {scanned && (
            <TouchableOpacity 
              style={[styles.button, { backgroundColor: '#555', width: '45%' }]} 
              onPress={() => {
                isLock.current = false; 
                setScanned(false);
              }}
            >
              <Text style={styles.buttonText}>Tiếp tục</Text>
            </TouchableOpacity>
          )}
  
          {/* Nút Trang chủ: Để ở ngoài khối {scanned && ...} để nó luôn hiện */}
          <TouchableOpacity 
            style={[
              styles.button, 
              { backgroundColor: '#FF9800', width: scanned ? '45%' : '100%' } // Tự giãn nếu không có nút Tiếp tục
            ]} 
            onPress={() => onNavigateToDashboard?.()}
          >
            <Text style={styles.buttonText}>Trang chủ</Text>
          </TouchableOpacity>
        </View>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
    container: { flex: 1, backgroundColor: '#121212', alignItems: 'center', justifyContent: 'center', width: '100%' },
    title: { fontSize: 20, fontWeight: 'bold', color: '#00ff00', marginBottom: 20, letterSpacing: 2 },
    cameraContainer: { width: 280, height: 280, borderRadius: 20, overflow: 'hidden', position: 'relative', borderWidth: 1, borderColor: '#333' },
    overlayScanner: { position: 'absolute', top: '15%', left: '15%', width: '70%', height: '70%', borderWidth: 2, borderColor: '#00ff00', borderRadius: 10 },
    resultContainer: { width: '85%', marginTop: 25, padding: 15, borderRadius: 12, backgroundColor: '#1e1e1e', alignItems: 'center' },
    resultLabel: { color: '#888', fontSize: 13 },
    resultText: { color: '#fff', fontSize: 16, fontWeight: 'bold', marginVertical: 10, textAlign: 'center' },
    buttonRow: { flexDirection: 'row', justifyContent: 'space-between', width: '100%', marginTop: 10 },
    button: { paddingVertical: 10, borderRadius: 20, alignItems: 'center' },
    buttonText: { color: '#fff', fontWeight: 'bold' }
  });
  


