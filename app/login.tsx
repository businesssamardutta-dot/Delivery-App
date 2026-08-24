import React, { useState } from 'react';
import { View, Text, TextInput, TouchableOpacity, ActivityIndicator, Image, ScrollView, SafeAreaView } from 'react-native';
import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import { router } from 'expo-router';
import { AuthService } from '../src/services/auth.service';

// Form validation schema using Zod
const loginSchema = z.object({
  email: z.string().email('Please enter a valid email address'),
  password: z.string().min(6, 'Password must be at least 6 characters'),
});

type LoginFormValues = z.infer<typeof loginSchema>;

export default function LoginScreen() {
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  const { control, handleSubmit, formState: { errors } } = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: {
      email: '',
      password: '',
    },
  });

  const onSubmit = async (data: LoginFormValues) => {
    setIsLoading(true);
    setErrorMsg(null);
    try {
      const result = await AuthService.login(data.email, data.password);
      if (result.profile) {
        // Navigate to the main home tab screen upon successful verification
        router.replace('/(tabs)/home');
      } else {
        setErrorMsg('Authorization failed. Access restricted to delivery partners only.');
      }
    } catch (error: any) {
      setErrorMsg(error.message || 'Login failed. Please check your credentials.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <SafeAreaView className="flex-1 bg-[#F9FAF9]">
      <ScrollView contentContainerStyle={{ flexGrow: 1 }} keyboardShouldPersistTaps="handled">
        {/* Brand Header */}
        <View className="bg-[#1B5E20] py-12 px-6 items-center justify-center rounded-b-3xl shadow-md">
          <View className="w-16 h-16 bg-white rounded-full items-center justify-center shadow-lg mb-3">
            <Image 
              source={{ uri: 'https://images.unsplash.com/photo-1586880244406-556ebe35f28e?q=80&w=150&auto=format&fit=crop' }} 
              className="w-14 h-14 rounded-full"
            />
          </View>
          <Text className="text-white text-3xl font-black tracking-wide">Haribansho</Text>
          <Text className="text-green-100 text-sm font-medium mt-1">Delivery Agent Platform</Text>
        </View>

        {/* Input Form Fields Card */}
        <View className="px-6 py-8 flex-1 justify-center">
          <View className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100">
            <Text className="text-[#1A1C19] text-xl font-bold mb-6 text-center">Partner Sign In</Text>

            {/* Error Message Box */}
            {errorMsg && (
              <View className="bg-red-50 p-4 rounded-xl border border-red-100 mb-4">
                <Text className="text-red-700 text-xs font-semibold text-center leading-tight">
                  {errorMsg}
                </Text>
              </View>
            )}

            {/* Email Field */}
            <View className="mb-4">
              <Text className="text-gray-700 text-xs font-bold mb-1 uppercase tracking-wider">Email Address</Text>
              <Controller
                control={control}
                name="email"
                render={({ field: { onChange, onBlur, value } }) => (
                  <TextInput
                    className={`bg-[#F4F6F4] px-4 py-3 rounded-xl text-base text-gray-800 border ${errors.email ? 'border-red-400' : 'border-transparent'}`}
                    placeholder="partner@haribansho.com"
                    placeholderTextColor="#8C938C"
                    keyboardType="email-address"
                    autoCapitalize="none"
                    onBlur={onBlur}
                    onChangeText={onChange}
                    value={value}
                  />
                )}
              />
              {errors.email && (
                <Text className="text-red-500 text-xs mt-1 font-semibold">{errors.email.message}</Text>
              )}
            </View>

            {/* Password Field */}
            <View className="mb-6">
              <Text className="text-gray-700 text-xs font-bold mb-1 uppercase tracking-wider">Password</Text>
              <Controller
                control={control}
                name="password"
                render={({ field: { onChange, onBlur, value } }) => (
                  <TextInput
                    className={`bg-[#F4F6F4] px-4 py-3 rounded-xl text-base text-gray-800 border ${errors.password ? 'border-red-400' : 'border-transparent'}`}
                    placeholder="••••••••"
                    placeholderTextColor="#8C938C"
                    secureTextEntry
                    autoCapitalize="none"
                    onBlur={onBlur}
                    onChangeText={onChange}
                    value={value}
                  />
                )}
              />
              {errors.password && (
                <Text className="text-red-500 text-xs mt-1 font-semibold">{errors.password.message}</Text>
              )}
            </View>

            {/* Submit Action Button */}
            <TouchableOpacity
              onPress={handleSubmit(onSubmit)}
              disabled={isLoading}
              activeOpacity={0.8}
              className={`py-4 rounded-xl items-center justify-center shadow-sm ${isLoading ? 'bg-green-800' : 'bg-[#2E7D32]'}`}
            >
              {isLoading ? (
                <ActivityIndicator color="#ffffff" size="small" />
              ) : (
                <Text className="text-white text-base font-bold">Sign In Securely</Text>
              )}
            </TouchableOpacity>
          </View>
        </View>

        {/* Footer info */}
        <View className="py-6 items-center">
          <Text className="text-gray-400 text-xs font-medium">Haribansho Delivery Logistics • v1.0.0</Text>
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}
