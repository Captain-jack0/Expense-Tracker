import React from 'react';
import { useAuth } from '../contexts/AuthContext';

const DashboardPage: React.FC = () => {
  const { user, logout } = useAuth();

  const handleLogout = async () => {
    try {
      await logout();
    } catch (error) {
      console.error('Logout failed:', error);
    }
  };

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <header className="bg-white shadow-sm">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4 flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold text-gray-900">Dashboard</h1>
            <p className="text-sm text-gray-600">
              Welcome back, {user?.firstName} {user?.lastName}
            </p>
          </div>
          <button onClick={handleLogout} className="btn btn-secondary">
            Logout
          </button>
        </div>
      </header>

      {/* Main Content */}
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Stats Cards */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
          <div className="card">
            <h3 className="text-sm font-medium text-gray-600 mb-2">Total Balance</h3>
            <p className="text-3xl font-bold text-gray-900">$0.00</p>
            <p className="text-xs text-gray-500 mt-1">No accounts yet</p>
          </div>

          <div className="card">
            <h3 className="text-sm font-medium text-gray-600 mb-2">This Month Income</h3>
            <p className="text-3xl font-bold text-success">$0.00</p>
            <p className="text-xs text-gray-500 mt-1">No transactions yet</p>
          </div>

          <div className="card">
            <h3 className="text-sm font-medium text-gray-600 mb-2">This Month Expenses</h3>
            <p className="text-3xl font-bold text-danger">$0.00</p>
            <p className="text-xs text-gray-500 mt-1">No transactions yet</p>
          </div>

          <div className="card">
            <h3 className="text-sm font-medium text-gray-600 mb-2">Net Savings</h3>
            <p className="text-3xl font-bold text-primary-600">$0.00</p>
            <p className="text-xs text-gray-500 mt-1">No data yet</p>
          </div>
        </div>

        {/* Quick Actions */}
        <div className="card mb-8">
          <h2 className="text-xl font-semibold text-gray-900 mb-4">Quick Actions</h2>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <button className="btn btn-primary">
              + Add Transaction
            </button>
            <button className="btn btn-secondary">
              + Create Account
            </button>
            <button className="btn btn-secondary">
              + Set Goal
            </button>
          </div>
        </div>

        {/* Recent Transactions */}
        <div className="card">
          <h2 className="text-xl font-semibold text-gray-900 mb-4">Recent Transactions</h2>
          <div className="text-center py-12 text-gray-500">
            <svg
              className="mx-auto h-12 w-12 text-gray-400"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"
              />
            </svg>
            <p className="mt-2">No transactions yet</p>
            <p className="text-sm text-gray-400">Start by adding your first transaction</p>
          </div>
        </div>
      </main>
    </div>
  );
};

export default DashboardPage;
