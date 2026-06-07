"use client";
import { useState } from "react";
import { useForm, useFieldArray } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Plus, Trash2, UploadCloud, FileType2, FileText, Loader2, CheckCircle2 } from "lucide-react";
import { Button } from "@/components/ui/Button";
import type { Concert } from "@/types/concert";

const ticketSchema = z.object({
  name: z.string().min(1, "Tên hạng vé không được để trống"),
  price: z.number().min(1, "Giá phải lớn hơn 0"),
  totalQuantity: z.number().min(1, "Số lượng phải lớn hơn 0"),
  maxPerUser: z.number().min(1, "Tối thiểu 1 vé/người"),
  saleStartTime: z.string().min(1, "Chọn thời gian mở bán"),
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
});

type FormData = z.infer<typeof formSchema>;

interface ConcertFormProps {
  initialData?: Concert;
  onSubmit: (data: Record<string, unknown>) => Promise<void>;
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
        }
      : { ticketCategories: [{ name: "", price: 0, totalQuantity: 0, maxPerUser: 2, saleStartTime: "" }] },
  });

  const { fields, append, remove } = useFieldArray({ control, name: "ticketCategories" });

  const [svgFile, setSvgFile] = useState<File | null>(null);
  const [pdfFile, setPdfFile] = useState<File | null>(null);
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
      // Simulate AI Extraction
      setIsExtractingPdf(true);
      setExtractionComplete(false);
      setTimeout(() => {
        setIsExtractingPdf(false);
        setExtractionComplete(true);
      }, 3000);
    }
  };

  const onValid = async (data: FormData) => {
    // We would normally upload svgFile and pdfFile here
    await onSubmit(data as unknown as Record<string, unknown>);
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
      </div>

      {/* File Uploads */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* SVG Upload */}
        <div className="border-2 border-dashed border-border rounded-2xl p-6 text-center hover:bg-secondary/50 transition-colors relative cursor-pointer">
          <input 
            type="file" 
            accept=".svg" 
            onChange={handleSvgChange}
            className="absolute inset-0 w-full h-full opacity-0 cursor-pointer" 
          />
          <div className="flex flex-col items-center justify-center space-y-3 pointer-events-none">
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
            accept=".pdf" 
            onChange={handlePdfChange}
            className="absolute inset-0 w-full h-full opacity-0 cursor-pointer" 
          />
          <div className="flex flex-col items-center justify-center space-y-3 pointer-events-none">
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

      <Button type="submit" loading={isSubmitting} size="lg" className="w-full">
        {initialData ? "Cập nhật sự kiện" : "Tạo sự kiện"}
      </Button>
    </form>
  );
}
