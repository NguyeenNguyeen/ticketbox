"use client";

import { useEffect, useState } from "react";
import { api } from "@/lib/api";
import { Trash2 } from "lucide-react";

interface UserDto {
  id: string;
  username: string;
  fullName?: string;
  email?: string;
  role: string;
}

export default function AdminUsersPage() {
  const [users, setUsers] = useState<UserDto[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadUsers();
  }, []);

  async function loadUsers() {
    setLoading(true);
    try {
      const data = await api.get<UserDto[]>("/admin/users");
      setUsers(data || []);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  }

  async function handleDelete(id: string) {
    if (!window.confirm("Bạn có chắc chắn muốn xoá người dùng này?")) return;
    try {
      await api.delete(`/admin/users/${id}`);
      await loadUsers();
    } catch (e: any) {
      alert("Lỗi khi xoá: " + (e.message || "Unknown error"));
    }
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-8">
        <h1 className="text-3xl font-bold">Quản lý Người dùng</h1>
      </div>

      {loading ? (
        <div>Đang tải...</div>
      ) : (
        <div className="bg-white rounded-2xl border border-border overflow-hidden">
          <table className="w-full text-sm text-left">
            <thead className="bg-secondary text-muted-foreground">
              <tr>
                <th className="px-6 py-4 font-medium">ID</th>
                <th className="px-6 py-4 font-medium">Tên đăng nhập</th>
                <th className="px-6 py-4 font-medium">Họ tên</th>
                <th className="px-6 py-4 font-medium">Email</th>
                <th className="px-6 py-4 font-medium">Vai trò</th>
                <th className="px-6 py-4 font-medium text-right">Thao tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border">
              {users.map((user) => (
                <tr key={user.id} className="hover:bg-secondary/50 transition-colors">
                  <td className="px-6 py-4">{user.id}</td>
                  <td className="px-6 py-4 font-medium">{user.username}</td>
                  <td className="px-6 py-4">{user.fullName || "-"}</td>
                  <td className="px-6 py-4">{user.email || "-"}</td>
                  <td className="px-6 py-4">
                    <span className="px-2 py-1 rounded-full text-xs font-medium bg-blue-50 text-blue-600">
                      {user.role}
                    </span>
                  </td>
                  <td className="px-6 py-4 text-right">
                    <button onClick={() => handleDelete(user.id)} className="text-destructive hover:text-destructive/80 p-2">
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </td>
                </tr>
              ))}
              {users.length === 0 && (
                <tr>
                  <td colSpan={6} className="px-6 py-8 text-center text-muted-foreground">
                    Không có người dùng nào.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
