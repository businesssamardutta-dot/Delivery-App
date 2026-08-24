import React, { useEffect, useState, useCallback } from 'react';
import { View, Text, ScrollView, RefreshControl, TouchableOpacity, ActivityIndicator, Image, SafeAreaView } from 'react-native';
import { OrdersService, DashboardMetrics, Order } from '../../src/services/orders.service';
import { AuthService, DeliveryBoy } from '../../src/services/auth.service';
import { router } from 'expo-router';

export default function HomeDashboardScreen() {
  const [metrics, setMetrics] = useState<DashboardMetrics | null>(null);
  const [profile, setProfile] = useState<DeliveryBoy | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [isOnline, setIsOnline] = useState(true);

  const loadData = useCallback(async (showSkeleton = true) => {
    if (showSkeleton) setIsLoading(true);
    setErrorMsg(null);
    try {
      // 1. Fetch Auth User and Profile details
      const sessionData = await AuthService.getCurrentSession();
      if (!sessionData.session || !sessionData.profile) {
        // Unauthenticated, redirect to login page
        router.replace('/login');
        return;
      }
      setProfile(sessionData.profile);
      setIsOnline(sessionData.profile.is_online);

      // 2. Fetch live dashboard stats from DB
      const liveStats = await OrdersService.fetchDashboardMetrics(sessionData.profile.delivery_boy_id);
      setMetrics(liveStats);
    } catch (err: any) {
      setErrorMsg(err.message || 'Unable to update metrics. Please try again.');
    } finally {
      setIsLoading(false);
      setIsRefreshing(false);
    }
  }, []);

  useEffect(() => {
    loadData();

    // Set up active 4-second polling interval for real-time app-to-web updates
    const pollInterval = setInterval(() => {
      loadData(false);
    }, 4000);

    return () => clearInterval(pollInterval);
  }, [loadData]);

  const onRefresh = () => {
    setIsRefreshing(true);
    loadData(false);
  };

  const handleLogout = async () => {
    try {
      await AuthService.logout();
      router.replace('/login');
    } catch (err) {
      console.error('Logout error', err);
    }
  };

  // Render a skeleton loader while loading initial metrics
  if (isLoading) {
    return (
      <SafeAreaView className="flex-1 bg-[#F9FAF9] justify-center items-center">
        <ActivityIndicator size="large" color="#2E7D32" />
        <Text className="text-gray-500 font-medium mt-4">Retrieving active profile & live metrics...</Text>
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView className="flex-1 bg-[#F9FAF9]">
      {/* Brand Navigation Header */}
      <View className="bg-[#1B5E20] pt-6 pb-8 px-6 rounded-b-3xl shadow-md">
        <View className="flex-row justify-between items-center">
          <View>
            <Text className="text-white text-2xl font-black">Haribansho</Text>
            <Text className="text-green-100 text-xs mt-0.5">Live Delivery Logistics Partner</Text>
          </View>
          <TouchableOpacity 
            onPress={handleLogout}
            className="bg-white/10 px-4 py-2 rounded-full border border-white/20 active:bg-white/20"
          >
            <Text className="text-white text-xs font-bold">Sign Out</Text>
          </TouchableOpacity>
        </View>

        {/* Profile Details Bar */}
        {profile && (
          <View className="bg-white p-4 rounded-2xl shadow-sm border border-gray-100 mt-6 flex-row justify-between items-center">
            <View className="flex-row items-center space-x-3">
              <View className="w-12 h-12 bg-green-50 rounded-full items-center justify-center">
                <Text className="text-[#2E7D32] text-lg font-bold">
                  {profile.name ? profile.name.charAt(0).toUpperCase() : 'P'}
                </Text>
              </View>
              <View>
                <Text className="text-[#1A1C19] text-base font-bold">{profile.name}</Text>
                <Text className="text-gray-400 text-xs">ID: {profile.delivery_boy_id}</Text>
              </View>
            </View>

            {/* Online Badge status indicator */}
            <View className={`px-4 py-1.5 rounded-full ${isOnline ? 'bg-green-100 border border-green-200' : 'bg-gray-100 border border-gray-200'}`}>
              <Text className={`text-xs font-bold ${isOnline ? 'text-green-800' : 'text-gray-500'}`}>
                {isOnline ? 'ONLINE' : 'OFFLINE'}
              </Text>
            </View>
          </View>
        )}
      </View>

      <ScrollView
        contentContainerStyle={{ flexGrow: 1, paddingBottom: 32 }}
        refreshControl={
          <RefreshControl refreshing={isRefreshing} onRefresh={onRefresh} colors={['#2E7D32']} />
        }
      >
        <View className="px-6 mt-8">
          <Text className="text-[#1A1C19] text-lg font-black mb-4">Today's Deliveries Overview</Text>

          {/* Render Error Alert Block */}
          {errorMsg && (
            <View className="bg-red-50 p-4 rounded-xl border border-red-100 mb-6">
              <Text className="text-red-700 text-xs font-semibold text-center leading-tight">
                {errorMsg}
              </Text>
              <TouchableOpacity onPress={() => loadData(true)} className="mt-2 items-center">
                <Text className="text-red-800 text-xs font-bold underline">Retry Network Connection</Text>
              </TouchableOpacity>
            </View>
          )}

          {/* Stat metrics cards grid */}
          {metrics && (
            <View className="flex-row space-x-3 mb-8">
              {/* Assigned stat card */}
              <View className="flex-1 bg-white p-4 rounded-2xl shadow-sm border border-gray-100 items-center justify-center">
                <View className="w-10 h-10 bg-blue-50 rounded-full items-center justify-center mb-2">
                  <Text className="text-blue-600 text-lg">📦</Text>
                </View>
                <Text className="text-2xl font-black text-[#1A1C19]">{String(metrics.assigned).padStart(2, '0')}</Text>
                <Text className="text-gray-400 text-xs font-bold mt-1 text-center">Assigned</Text>
              </View>

              {/* In Progress stat card */}
              <View className="flex-1 bg-white p-4 rounded-2xl shadow-sm border border-gray-100 items-center justify-center">
                <View className="w-10 h-10 bg-amber-50 rounded-full items-center justify-center mb-2">
                  <Text className="text-amber-600 text-lg">🚲</Text>
                </View>
                <Text className="text-2xl font-black text-[#1A1C19]">{String(metrics.active).padStart(2, '0')}</Text>
                <Text className="text-gray-400 text-xs font-bold mt-1 text-center">Active</Text>
              </View>

              {/* Completed stat card */}
              <View className="flex-1 bg-white p-4 rounded-2xl shadow-sm border border-gray-100 items-center justify-center">
                <View className="w-10 h-10 bg-green-50 rounded-full items-center justify-center mb-2">
                  <Text className="text-green-600 text-lg">✅</Text>
                </View>
                <Text className="text-2xl font-black text-[#1A1C19]">{String(metrics.completed).padStart(2, '0')}</Text>
                <Text className="text-gray-400 text-xs font-bold mt-1 text-center">Completed</Text>
              </View>
            </View>
          )}

          {/* Active Deliveries Banner Info or Empty Placeholder State */}
          <Text className="text-[#1A1C19] text-base font-black mb-4">Pending Tasks</Text>

          {metrics && metrics.assigned === 0 && metrics.active === 0 ? (
            <View className="bg-white p-8 rounded-2xl border border-gray-100 shadow-sm items-center justify-center mt-2">
              <View className="w-16 h-16 bg-green-50 rounded-full items-center justify-center mb-4">
                <Text className="text-2xl">🎉</Text>
              </View>
              <Text className="text-[#1A1C19] text-base font-bold text-center">All Caught Up!</Text>
              <Text className="text-gray-400 text-xs font-medium text-center leading-relaxed mt-1 px-4">
                You have no pending or active delivery assignments right now. Pull down to refresh or check back later!
              </Text>
            </View>
          ) : (
            <View className="bg-green-50 border border-green-100 rounded-2xl p-5 shadow-sm">
              <Text className="text-[#2E7D32] text-sm font-black mb-1">Active Deliveries Scheduled</Text>
              <Text className="text-green-700 text-xs leading-relaxed">
                You have new assignments. Visit the Orders tab in your delivery app screen to review directions, customer details, and initiate delivery workflows.
              </Text>
              <TouchableOpacity 
                onPress={() => router.push('/(tabs)/orders' as any)}
                className="bg-[#2E7D32] mt-4 py-3 rounded-xl items-center justify-center shadow-sm"
              >
                <Text className="text-white text-xs font-bold">Go to Orders</Text>
              </TouchableOpacity>
            </View>
          )}
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}
