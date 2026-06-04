import type { Concert } from "@/types/concert";

export const mockConcerts: Concert[] = [
  {
    id: "concert-1",
    title: "Anh Trai Say Hi - Live Concert 2026",
    description:
      "Đêm nhạc đỉnh cao quy tụ các Anh Trai hot nhất mùa 2026. Sân khấu hoành tráng với hiệu ứng ánh sáng laser 360°, màn hình LED khổng lồ và hàng nghìn drone trình diễn trên bầu trời Mỹ Đình.",
    artistBio:
      "Các nghệ sĩ hàng đầu Gen Z Việt Nam — mang đến những bản hit triệu view và phần trình diễn không thể bỏ lỡ.",
    artists: ["HIEUTHUHAI", "Rhyder", "HURRYKNG", "Negav", "Quang Hùng MasterD"],
    venue: "Sân vận động Mỹ Đình",
    address: "Đường Lê Đức Thọ, Nam Từ Liêm, Hà Nội",
    date: "2026-12-20T19:30:00+07:00",
    doors: "18:00",
    showTime: "19:30",
    bannerUrl: "/concert-anh-trai-say-hi.png",
    status: "ON_SALE",
    ticketCategories: [
      { id: "tc-1-1", name: "SVIP", price: 5000000, totalQuantity: 200, availableQuantity: 142, maxPerUser: 2, saleStartTime: "2026-11-01T10:00:00+07:00", color: "#F59E0B" },
      { id: "tc-1-2", name: "VIP", price: 3500000, totalQuantity: 500, availableQuantity: 318, maxPerUser: 2, saleStartTime: "2026-11-01T10:00:00+07:00", color: "#7C3AED" },
      { id: "tc-1-3", name: "CAT1", price: 2000000, totalQuantity: 1000, availableQuantity: 756, maxPerUser: 4, saleStartTime: "2026-11-01T10:00:00+07:00", color: "#3B82F6" },
      { id: "tc-1-4", name: "CAT2", price: 1200000, totalQuantity: 1500, availableQuantity: 1203, maxPerUser: 4, saleStartTime: "2026-11-01T10:00:00+07:00", color: "#10B981" },
      { id: "tc-1-5", name: "GA", price: 800000, totalQuantity: 3000, availableQuantity: 2541, maxPerUser: 4, saleStartTime: "2026-11-01T10:00:00+07:00", color: "#9CA3AF" },
    ],
    createdAt: "2026-10-01T00:00:00+07:00",
    updatedAt: "2026-11-15T00:00:00+07:00",
  },
  {
    id: "concert-2",
    title: "Anh Trai Vượt Ngàn Chông Gai - The Show",
    description:
      "Huyền thoại trở lại! Đêm nhạc của những giọng ca vàng đã đồng hành cùng nhiều thế hệ khán giả Việt Nam. Thưởng thức loạt hit bất hủ trên sân khấu đẳng cấp quốc tế.",
    artistBio:
      "Những giọng ca hàng đầu Vpop — mỗi người là một huyền thoại với hàng triệu fan trung thành.",
    artists: ["Tuấn Hưng", "Bằng Kiều", "Đàm Vĩnh Hưng", "Quang Dũng"],
    venue: "Nhà thi đấu Phú Thọ",
    address: "1 Lữ Gia, Quận 11, TP. Hồ Chí Minh",
    date: "2027-01-15T20:00:00+07:00",
    doors: "18:30",
    showTime: "20:00",
    bannerUrl: "/concert-anh-trai-vuot-ngan.png",
    status: "UPCOMING",
    ticketCategories: [
      { id: "tc-2-1", name: "SVIP", price: 6000000, totalQuantity: 150, availableQuantity: 150, maxPerUser: 2, saleStartTime: "2026-12-15T10:00:00+07:00", color: "#F59E0B" },
      { id: "tc-2-2", name: "VIP", price: 4000000, totalQuantity: 400, availableQuantity: 400, maxPerUser: 2, saleStartTime: "2026-12-15T10:00:00+07:00", color: "#7C3AED" },
      { id: "tc-2-3", name: "CAT1", price: 2500000, totalQuantity: 800, availableQuantity: 800, maxPerUser: 4, saleStartTime: "2026-12-15T10:00:00+07:00", color: "#3B82F6" },
      { id: "tc-2-4", name: "CAT2", price: 1500000, totalQuantity: 1200, availableQuantity: 1200, maxPerUser: 4, saleStartTime: "2026-12-15T10:00:00+07:00", color: "#10B981" },
      { id: "tc-2-5", name: "GA", price: 900000, totalQuantity: 2500, availableQuantity: 2500, maxPerUser: 4, saleStartTime: "2026-12-15T10:00:00+07:00", color: "#9CA3AF" },
    ],
    createdAt: "2026-10-15T00:00:00+07:00",
    updatedAt: "2026-11-20T00:00:00+07:00",
  },
  {
    id: "concert-3",
    title: "Em Xinh Say Hi - Valentine Concert",
    description:
      "Đêm nhạc ngọt ngào dành riêng cho mùa Valentine. Những giọng ca nữ trẻ tài năng nhất sẽ mang đến không gian âm nhạc lãng mạn, đầy cảm xúc giữa lòng Thủ đô.",
    artistBio:
      "Thế hệ nữ nghệ sĩ mới đầy tài năng — những giọng ca ngọt ngào chinh phục hàng triệu trái tim.",
    artists: ["Amee", "Juky San", "Hoàng Duyên", "Vũ Cát Tường"],
    venue: "Trung tâm Hội nghị Quốc gia",
    address: "Đường Phạm Hùng, Mễ Trì, Nam Từ Liêm, Hà Nội",
    date: "2027-02-14T19:00:00+07:00",
    doors: "17:30",
    showTime: "19:00",
    bannerUrl: "/concert-em-xinh-say-hi.png",
    status: "ON_SALE",
    ticketCategories: [
      { id: "tc-3-1", name: "SVIP", price: 4000000, totalQuantity: 100, availableQuantity: 67, maxPerUser: 2, saleStartTime: "2027-01-01T10:00:00+07:00", color: "#F59E0B" },
      { id: "tc-3-2", name: "VIP", price: 2800000, totalQuantity: 300, availableQuantity: 198, maxPerUser: 2, saleStartTime: "2027-01-01T10:00:00+07:00", color: "#7C3AED" },
      { id: "tc-3-3", name: "CAT1", price: 1800000, totalQuantity: 600, availableQuantity: 445, maxPerUser: 4, saleStartTime: "2027-01-01T10:00:00+07:00", color: "#3B82F6" },
      { id: "tc-3-4", name: "CAT2", price: 1000000, totalQuantity: 800, availableQuantity: 672, maxPerUser: 4, saleStartTime: "2027-01-01T10:00:00+07:00", color: "#10B981" },
      { id: "tc-3-5", name: "GA", price: 600000, totalQuantity: 2000, availableQuantity: 1823, maxPerUser: 4, saleStartTime: "2027-01-01T10:00:00+07:00", color: "#9CA3AF" },
    ],
    createdAt: "2026-11-01T00:00:00+07:00",
    updatedAt: "2026-12-20T00:00:00+07:00",
  },
  {
    id: "concert-4",
    title: "Chị Đẹp Đạp Gió Rẽ Sóng - Đêm Gala",
    description:
      "Đêm nhạc tôn vinh sức mạnh và vẻ đẹp của những nữ nghệ sĩ hàng đầu. Sân khấu Thống Nhất sẽ bùng cháy với những màn trình diễn đẳng cấp, kết hợp vũ đạo và hiệu ứng ánh sáng mãn nhãn.",
    artistBio:
      "Những diva hàng đầu Việt Nam — mỗi người là biểu tượng âm nhạc của một thời đại.",
    artists: ["Mỹ Tâm", "Thu Minh", "Hồ Ngọc Hà", "Thanh Lam"],
    venue: "Sân vận động Thống Nhất",
    address: "138 Đào Duy Từ, Quận 10, TP. Hồ Chí Minh",
    date: "2027-03-08T20:00:00+07:00",
    doors: "18:00",
    showTime: "20:00",
    bannerUrl: "/concert-chi-dep-dap-gio.png",
    status: "UPCOMING",
    ticketCategories: [
      { id: "tc-4-1", name: "SVIP", price: 7000000, totalQuantity: 100, availableQuantity: 100, maxPerUser: 2, saleStartTime: "2027-02-01T10:00:00+07:00", color: "#F59E0B" },
      { id: "tc-4-2", name: "VIP", price: 5000000, totalQuantity: 300, availableQuantity: 300, maxPerUser: 2, saleStartTime: "2027-02-01T10:00:00+07:00", color: "#7C3AED" },
      { id: "tc-4-3", name: "CAT1", price: 3000000, totalQuantity: 700, availableQuantity: 700, maxPerUser: 4, saleStartTime: "2027-02-01T10:00:00+07:00", color: "#3B82F6" },
      { id: "tc-4-4", name: "CAT2", price: 1800000, totalQuantity: 1000, availableQuantity: 1000, maxPerUser: 4, saleStartTime: "2027-02-01T10:00:00+07:00", color: "#10B981" },
      { id: "tc-4-5", name: "GA", price: 1000000, totalQuantity: 2000, availableQuantity: 2000, maxPerUser: 4, saleStartTime: "2027-02-01T10:00:00+07:00", color: "#9CA3AF" },
    ],
    createdAt: "2026-12-01T00:00:00+07:00",
    updatedAt: "2027-01-10T00:00:00+07:00",
  },
];
