"use client";
import { useState } from "react";
import { useForm, useFieldArray } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Plus, Trash2, UploadCloud, FileType2, FileText, Loader2, CheckCircle2, Image } from "lucide-react";
import { Button } from "@/components/ui/Button";
import { api } from "@/lib/api";
import type { Concert } from "@/types/concert";

const ticketSchema = z.object({
  name: z.string().min(1, "Tên hạng vé không được để trống"),
  price: z.number().min(1, "Giá phải lớn hơn 0"),
  totalQuantity: z.number().min(1, "Số lượng phải lớn hơn 0"),
  maxPerUser: z.number().min(1, "Tối thiểu 1 vé/người"),
  saleStartTime: z.string().min(1, "Chọn thời gian mở bán"),
});

const artistSchema = z.object({
  id: z.string().optional(),
  name: z.string().min(1, "Tên nghệ sĩ không được để trống"),
  avatarUrl: z.string().optional(),
  bio: z.string().optional(),
});

const formSchema = z.object({
  title: z.string().min(1, "Tên sự kiện không được để trống"),
  description: z.string().min(1, "Mô tả không được để trống"),
  venue: z.string().min(1, "Địa điểm không được để trống"),
  address: z.string().min(1, "Địa chỉ không được để trống"),
  date: z.string().min(1, "Chọn ngày diễn"),
  doors: z.string().min(1, "Chọn giờ mở cửa"),
  showTime: z.string().min(1, "Chọn giờ biểu diễn"),
  ticketCategories: z.array(ticketSchema).min(1, "Cần ít nhất 1 hạng vé"),
  forcedStatus: z.string().optional(),
  artists: z.array(artistSchema).optional(),
});

type FormData = z.infer<typeof formSchema>;

interface ConcertFormProps {
  initialData?: Concert;
  onSubmit: (data: Record<string, unknown>) => Promise<string | void>;
}

export function ConcertForm({ initialData, onSubmit }: ConcertFormProps) {
  const { register, control, handleSubmit, formState: { errors, isSubmitting } } = useForm<FormData>({
    resolver: zodResolver(formSchema),
    defaultValues: initialData
      ? {
          title: initialData.title,
          description: initialData.description,
          venue: initialData.venue,
          address: initialData.address,
          date: initialData.date.split("T")[0],
          doors: initialData.doors,
          showTime: initialData.showTime,
          ticketCategories: initialData.ticketCategories.map((tc) => ({
            name: tc.name, price: tc.price, totalQuantity: tc.totalQuantity,
            maxPerUser: tc.maxPerUser, saleStartTime: tc.saleStartTime.split("T")[0],
          })),
          artists: initialData.artists?.map((a) => ({
            id: a.id, name: a.name, avatarUrl: a.avatarUrl || "", bio: a.bio || "",
          })) || [],
          forcedStatus: initialData.forcedStatus || "",
        }
      : { 
          ticketCategories: [{ name: "", price: 0, totalQuantity: 0, maxPerUser: 2, saleStartTime: "" }],
          forcedStatus: "",
          artists: []
        },
  });

  const { fields, append, remove } = useFieldArray({ control, name: "ticketCategories" });
  const { fields: artistFields, append: appendArtist, remove: removeArtist } = useFieldArray({ control, name: "artists" });

  const [svgFile, setSvgFile] = useState<File | null>(null);
  const [pdfFile, setPdfFile] = useState<File | null>(null);
  const [bannerFile, setBannerFile] = useState<File | null>(null);
  const [isExtractingPdf, setIsExtractingPdf] = useState(false);
  const [extractionComplete, setExtractionComplete] = useState(false);

  const handleSvgChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      setSvgFile(e.target.files[0]);
    }
  };

  const handlePdfChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      setPdfFile(e.target.files[0]);
      setIsExtractingPdf(false);
      setExtractionComplete(false);
    }
  };

  const handleBannerChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      setBannerFile(e.target.files[0]);
    }
  };

  const onValid = async (data: FormData) => {
    try {
      const returnedId = await onSubmit(data as unknown as Record<string, unknown>);
      const cid = returnedId || initialData?.id || "1";
      
      if (svgFile) {
        try {
          await api.uploadFile(`/admin/concerts/${cid}/upload-map`, svgFile, "file");
        } catch (err: any) {
          console.error("Failed to upload SVG", err);
          alert("Lỗi upload sơ đồ ghế: " + err.message);
        }
      }

      if (bannerFile) {
        try {
          await api.uploadFile(`/admin/concerts/${cid}/upload-banner`, bannerFile, "file");
        } catch (err: any) {
          console.error("Failed to upload banner", err);
          alert("Lỗi upload ảnh nền: " + err.message);
        }
      }

      if (pdfFile) {
        setIsExtractingPdf(true);
        setExtractionComplete(false);
        try {
          // Upload PDF
          const { jobId } = await api.uploadFile<{jobId: string}>(`/admin/concerts/${cid}/upload-bio`, pdfFile, "file");
          
          // Poll for status
          let isDone = false;
          let retries = 0;
          while (!isDone && retries < 30) {
            await new Promise(r => setTimeout(r, 2000));
            try {
              const statusData = await api.get<{status: string, errorReason: string}>(`/admin/concerts/ai-jobs/${jobId}`);
              if (statusData.status === "COMPLETED") {
                isDone = true;
                setExtractionComplete(true);
              } else if (statusData.status === "FAILED") {
                isDone = true;
                console.error("AI Bio extraction failed:", statusData.errorReason);
                alert("Lỗi khi phân tích PDF: " + (statusData.errorReason || "Unknown error"));
              }
            } catch (err) {
              console.error("Error polling AI status", err);
              if (retries > 5) {
                isDone = true;
                alert("Lỗi mạng khi kiểm tra trạng thái PDF. Vui lòng thử lại sau.");
              }
            }
            retries++;
          }
        } catch (err: any) {
          console.error("Failed to upload PDF", err);
          alert("Lỗi upload PDF: " + err.message);
        } finally {
          setIsExtractingPdf(false);
        }
      }
      
      // Redirect after everything is done
      window.location.href = "/admin/concerts";
    } catch (err) {
      console.error(err);
    }
  };

  const inputClass = "w-full px-4 py-3 rounded-xl border border-border bg-white focus:outline-none focus:ring-2 focus:ring-primary/20 focus:border-primary transition-all text-sm";

  return (
    <form onSubmit={handleSubmit(onValid)} className="space-y-6">
      {/* Basic info */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div className="md:col-span-2">
          <label className="block text-sm font-medium mb-2">Tên sự kiện</label>
          <input {...register("title")} className={inputClass} placeholder="VD: Anh Trai Say Hi" />
          {errors.title && <p className="text-sm text-destructive mt-1">{errors.title.message}</p>}
        </div>
        <div className="md:col-span-2">
          <label className="block text-sm font-medium mb-2">Mô tả</label>
          <textarea {...register("description")} rows={3} className={inputClass} placeholder="Mô tả chi tiết sự kiện..." />
          {errors.description && <p className="text-sm text-destructive mt-1">{errors.description.message}</p>}
        </div>
        <div>
          <label className="block text-sm font-medium mb-2">Địa điểm</label>
          <input {...register("venue")} className={inputClass} placeholder="VD: Sân vận động Mỹ Đình" />
          {errors.venue && <p className="text-sm text-destructive mt-1">{errors.venue.message}</p>}
        </div>
        <div>
          <label className="block text-sm font-medium mb-2">Địa chỉ</label>
          <input {...register("address")} className={inputClass} placeholder="Đường, Quận, Thành phố" />
          {errors.address && <p className="text-sm text-destructive mt-1">{errors.address.message}</p>}
        </div>
        <div>
          <label className="block text-sm font-medium mb-2">Ngày diễn</label>
          <input type="date" {...register("date")} className={inputClass} />
          {errors.date && <p className="text-sm text-destructive mt-1">{errors.date.message}</p>}
        </div>
        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className="block text-sm font-medium mb-2">Mở cửa</label>
            <input type="time" {...register("doors")} className={inputClass} />
          </div>
          <div>
            <label className="block text-sm font-medium mb-2">Bắt đầu</label>
            <input type="time" {...register("showTime")} className={inputClass} />
          </div>
        </div>
        <div className="md:col-span-2">
          <label className="block text-sm font-medium mb-2">Trạng thái mở bán</label>
          <select {...register("forcedStatus")} className={inputClass}>
            <option value="">Tự động (theo thời gian)</option>
            <option value="UPCOMING">Sắp mở bán</option>
            <option value="ON_SALE">Đang mở bán</option>
          </select>
          <p className="text-xs text-muted-foreground mt-1">
            Chọn trạng thái nếu muốn ép sự kiện mở bán thủ công. Nếu chọn "Tự động", hệ thống sẽ dựa vào "Ngày mở bán" của hạng vé để tính toán.
          </p>
        </div>
      </div>

      {/* File Uploads */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {/* Banner Upload */}
        <div className="border-2 border-dashed border-border rounded-2xl p-6 text-center hover:bg-secondary/50 transition-colors relative cursor-pointer overflow-hidden group">
          <input 
            type="file" 
            accept="image/*"
            onClick={(e) => { (e.target as HTMLInputElement).value = ""; }}
            onChange={handleBannerChange}
            className="absolute inset-0 w-full h-full opacity-0 cursor-pointer z-20" 
          />
          
          {/* Background Preview */}
          {bannerFile ? (
            <div className="absolute inset-0 z-0">
              <img src={URL.createObjectURL(bannerFile)} alt="Preview" className="w-full h-full object-cover opacity-30" />
            </div>
          ) : initialData?.bannerUrl ? (
            <div className="absolute inset-0 z-0">
              <img src={initialData.bannerUrl} alt="Current Banner" className="w-full h-full object-cover opacity-30" />
            </div>
          ) : null}

          <div className="flex flex-col items-center justify-center space-y-3 pointer-events-none relative z-10">
            <div className="w-12 h-12 bg-background/80 backdrop-blur-sm text-primary rounded-full flex items-center justify-center shadow-sm">
              <Image className="w-6 h-6" />
            </div>
            <div className="bg-background/80 backdrop-blur-sm px-3 py-1 rounded-lg shadow-sm inline-block">
              <p className="font-medium text-foreground">Ảnh nền (Banner)</p>
              <p className="text-sm text-muted-foreground mt-1">
                {bannerFile ? bannerFile.name : "Kéo thả/click để tải lên ảnh mới"}
              </p>
            </div>
          </div>
        </div>

        {/* SVG Upload */}
        <div className="border-2 border-dashed border-border rounded-2xl p-6 text-center hover:bg-secondary/50 transition-colors relative cursor-pointer">
          <input 
            type="file" 
            onClick={(e) => { (e.target as HTMLInputElement).value = ""; }}
            onChange={handleSvgChange}
            className="absolute inset-0 w-full h-full opacity-0 cursor-pointer z-10" 
          />
          <div className="flex flex-col items-center justify-center space-y-3 pointer-events-none relative z-0">
            <div className="w-12 h-12 bg-primary/10 text-primary rounded-full flex items-center justify-center">
              <FileType2 className="w-6 h-6" />
            </div>
            <div>
              <p className="font-medium text-foreground">Sơ đồ ghế (SVG)</p>
              <p className="text-sm text-muted-foreground mt-1">
                {svgFile ? svgFile.name : "Kéo thả hoặc click để tải lên file .svg"}
              </p>
            </div>
          </div>
        </div>

        {/* PDF Upload */}
        <div className="border-2 border-dashed border-border rounded-2xl p-6 text-center hover:bg-secondary/50 transition-colors relative cursor-pointer">
          <input 
            type="file" 
            onClick={(e) => { (e.target as HTMLInputElement).value = ""; }}
            onChange={handlePdfChange}
            className="absolute inset-0 w-full h-full opacity-0 cursor-pointer z-10" 
          />
          <div className="flex flex-col items-center justify-center space-y-3 pointer-events-none relative z-0">
            <div className="w-12 h-12 bg-primary/10 text-primary rounded-full flex items-center justify-center">
              {isExtractingPdf ? (
                <Loader2 className="w-6 h-6 animate-spin" />
              ) : extractionComplete ? (
                <CheckCircle2 className="w-6 h-6 text-success" />
              ) : (
                <FileText className="w-6 h-6" />
              )}
            </div>
            <div>
              <p className="font-medium text-foreground">Press Kit Nghệ sĩ (PDF)</p>
              <p className="text-sm text-muted-foreground mt-1">
                {isExtractingPdf 
                  ? "AI đang phân tích và trích xuất tiểu sử..." 
                  : extractionComplete 
                    ? `Đã trích xuất thành công từ ${pdfFile?.name}` 
                    : pdfFile 
                      ? `Đã chọn: ${pdfFile.name} (Sẽ xử lý khi lưu)`
                      : "Tải lên hồ sơ PDF để AI sinh tiểu sử tự động"}
              </p>
            </div>
          </div>
        </div>
      </div>

      {/* Ticket categories */}
      <div>
        <div className="flex items-center justify-between mb-4">
          <h3 className="font-semibold">Hạng vé</h3>
          <Button type="button" variant="outline" size="sm" onClick={() => append({ name: "", price: 0, totalQuantity: 0, maxPerUser: 2, saleStartTime: "" })}>
            <Plus className="w-4 h-4" /> Thêm hạng vé
          </Button>
        </div>
        <div className="space-y-4">
          {fields.map((field, i) => (
            <div key={field.id} className="p-4 bg-secondary/50 rounded-xl space-y-3">
              <div className="flex items-center justify-between">
                <span className="text-sm font-medium">Hạng vé #{i + 1}</span>
                {fields.length > 1 && (
                  <button type="button" onClick={() => remove(i)} className="text-destructive hover:text-destructive/80">
                    <Trash2 className="w-4 h-4" />
                  </button>
                )}
              </div>
              <div className="grid grid-cols-2 md:grid-cols-5 gap-3">
                <div>
                  <label className="text-xs text-muted-foreground">Tên</label>
                  <input {...register(`ticketCategories.${i}.name`)} className={inputClass} placeholder="VD: SVIP" />
                </div>
                <div>
                  <label className="text-xs text-muted-foreground">Giá (₫)</label>
                  <input type="number" {...register(`ticketCategories.${i}.price`, { valueAsNumber: true })} className={inputClass} />
                </div>
                <div>
                  <label className="text-xs text-muted-foreground">Số lượng</label>
                  <input type="number" {...register(`ticketCategories.${i}.totalQuantity`, { valueAsNumber: true })} className={inputClass} />
                </div>
                <div>
                  <label className="text-xs text-muted-foreground">Max/người</label>
                  <input type="number" {...register(`ticketCategories.${i}.maxPerUser`, { valueAsNumber: true })} className={inputClass} />
                </div>
                <div>
                  <label className="text-xs text-muted-foreground">Mở bán</label>
                  <input type="date" {...register(`ticketCategories.${i}.saleStartTime`)} className={inputClass} />
                </div>
              </div>
            </div>
          ))}
        </div>
        {errors.ticketCategories && <p className="text-sm text-destructive mt-2">{errors.ticketCategories.message}</p>}
      </div>

      {/* Guest Artists */}
      <div>
        <div className="flex items-center justify-between mb-4">
          <h3 className="font-semibold">Nghệ sĩ khách mời (Thủ công)</h3>
          <Button type="button" variant="outline" size="sm" onClick={() => appendArtist({ name: "", avatarUrl: "", bio: "" })}>
            <Plus className="w-4 h-4" /> Thêm nghệ sĩ
          </Button>
        </div>
        <div className="space-y-4">
          {artistFields.map((field, i) => (
            <div key={field.id} className="p-4 bg-secondary/30 rounded-xl space-y-3 border border-border">
              <div className="flex items-center justify-between">
                <span className="text-sm font-medium">Nghệ sĩ #{i + 1}</span>
                <button type="button" onClick={() => removeArtist(i)} className="text-destructive hover:text-destructive/80">
                  <Trash2 className="w-4 h-4" />
                </button>
              </div>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label className="text-xs text-muted-foreground">Tên nghệ sĩ</label>
                  <input {...register(`artists.${i}.name`)} className={inputClass} placeholder="VD: Sơn Tùng M-TP" />
                  {errors.artists?.[i]?.name && <p className="text-xs text-destructive mt-1">{errors.artists[i]?.name?.message}</p>}
                </div>
                <div>
                  <label className="text-xs text-muted-foreground">URL Ảnh đại diện</label>
                  <input {...register(`artists.${i}.avatarUrl`)} className={inputClass} placeholder="https://..." />
                </div>
                <div className="md:col-span-2">
                  <label className="text-xs text-muted-foreground">Tiểu sử ngắn</label>
                  <textarea {...register(`artists.${i}.bio`)} rows={2} className={inputClass} placeholder="Giới thiệu về nghệ sĩ..." />
                </div>
                <input type="hidden" {...register(`artists.${i}.id`)} />
              </div>
            </div>
          ))}
          {artistFields.length === 0 && (
            <p className="text-sm text-muted-foreground text-center py-4 bg-secondary/20 rounded-xl border border-dashed border-border">
              Chưa có nghệ sĩ nào. Bạn có thể thêm thủ công hoặc dùng tính năng Upload PDF để AI tự động trích xuất.
            </p>
          )}
        </div>
      </div>

      <Button type="submit" loading={isSubmitting} size="lg" className="w-full">
        {initialData ? "Cập nhật sự kiện" : "Tạo sự kiện"}
      </Button>
    </form>
  );
}
