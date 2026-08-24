import { supabase } from '../lib/supabase';

export interface Order {
  order_id: string;
  order_status: string;
  payment_method: string;
  total_amount: number;
  delivery_address: string;
  customer_name: string;
  customer_phone: string;
  restaurant_name?: string;
  restaurant_address?: string;
  created_at: string;
}

export interface DeliveryAssignment {
  assignment_id: string;
  order_id: string;
  delivery_boy_id: string;
  assignment_status: string;
  assigned_at: string;
  accepted_at?: string;
  completed_at?: string;
  rejected_at?: string;
  rejection_reason?: string;
  order?: Order;
}

export interface DashboardMetrics {
  assigned: number;
  active: number;
  completed: number;
}

export class OrdersService {
  /**
   * Fetch dashboard metrics for the current delivery boy
   */
  static async fetchDashboardMetrics(deliveryBoyId: string): Promise<DashboardMetrics> {
    // 1. Fetch assigned orders (status: Assigned)
    const { count: assignedCount, error: assignedErr } = await supabase
      .from('01_delivery_assignments')
      .select('assignment_id', { count: 'exact', head: true })
      .eq('delivery_boy_id', deliveryBoyId)
      .eq('assignment_status', 'Assigned');

    if (assignedErr) throw new Error(`Assigned count error: ${assignedErr.message}`);

    // 2. Fetch active orders (status: Accepted, On The Way, Reached Customer)
    const { count: activeCount, error: activeErr } = await supabase
      .from('01_delivery_assignments')
      .select('assignment_id', { count: 'exact', head: true })
      .eq('delivery_boy_id', deliveryBoyId)
      .in('assignment_status', ['Accepted', 'On The Way', 'Reached Customer']);

    if (activeErr) throw new Error(`Active count error: ${activeErr.message}`);

    // 3. Fetch completed orders (status: Completed / Delivered)
    const { count: completedCount, error: completedErr } = await supabase
      .from('01_delivery_assignments')
      .select('assignment_id', { count: 'exact', head: true })
      .eq('delivery_boy_id', deliveryBoyId)
      .eq('assignment_status', 'Completed');

    if (completedErr) throw new Error(`Completed count error: ${completedErr.message}`);

    return {
      assigned: assignedCount || 0,
      active: activeCount || 0,
      completed: completedCount || 0,
    };
  }

  /**
   * Fetch all delivery assignments for the current delivery boy
   */
  static async fetchDeliveryAssignments(deliveryBoyId: string): Promise<DeliveryAssignment[]> {
    const { data, error } = await supabase
      .from('01_delivery_assignments')
      .select(`
        *,
        order:01_orders (*)
      `)
      .eq('delivery_boy_id', deliveryBoyId)
      .order('assigned_at', { ascending: false });

    if (error) {
      throw new Error(`Fetch assignments error: ${error.message}`);
    }

    return (data || []) as DeliveryAssignment[];
  }

  /**
   * Atomic RPC call to accept a delivery assignment and update order status to 'Accepted'
   */
  static async acceptDeliveryAssignment(orderId: string, deliveryBoyId: string): Promise<boolean> {
    const { data, error } = await supabase.rpc('accept_delivery_assignment', {
      p_order_id: orderId,
      p_delivery_boy_id: deliveryBoyId,
    });

    if (error) {
      throw new Error(`Failed to accept order atomically: ${error.message}`);
    }

    return !!data;
  }

  /**
   * Start delivery process (Status: On The Way)
   */
  static async startDelivery(orderId: string): Promise<void> {
    const { error: orderError } = await supabase
      .from('01_orders')
      .update({ order_status: 'On The Way' })
      .eq('order_id', orderId);

    if (orderError) throw orderError;

    const { error: assignmentError } = await supabase
      .from('01_delivery_assignments')
      .update({ assignment_status: 'On The Way' })
      .eq('order_id', orderId);

    if (assignmentError) throw assignmentError;
  }

  /**
   * Complete delivery process (Status: Delivered / Completed)
   */
  static async completeDelivery(orderId: string): Promise<void> {
    const now = new Date().toISOString();

    const { error: orderError } = await supabase
      .from('01_orders')
      .update({ order_status: 'Delivered' })
      .eq('order_id', orderId);

    if (orderError) throw orderError;

    const { error: assignmentError } = await supabase
      .from('01_delivery_assignments')
      .update({ 
        assignment_status: 'Completed',
        completed_at: now
      })
      .eq('order_id', orderId);

    if (assignmentError) throw assignmentError;
  }
}
