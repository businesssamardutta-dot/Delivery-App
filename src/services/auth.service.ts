import { supabase } from '../lib/supabase';

export interface DeliveryBoy {
  delivery_boy_id: string;
  name: string;
  email: string;
  phone: string;
  is_online: boolean;
  vehicle_type: string;
  vehicle_number: string;
  status: string;
}

export class AuthService {
  /**
   * Log in user, verify against auth.users, and check role in 01_delivery_boys & 01_users
   */
  static async login(email: string, password: string): Promise<{ session: any; profile: DeliveryBoy }> {
    // 1. Authenticate with Supabase Auth
    const { data: authData, error: authError } = await supabase.auth.signInWithPassword({
      email,
      password,
    });

    if (authError) {
      throw new Error(authError.message);
    }

    if (!authData.user) {
      throw new Error('Authentication failed: user not found');
    }

    let deliveryBoyData: any = null;
    let userProfileData: any = null;

    // 2. Perform a join query between 01_delivery_boys and 01_users
    // Try relational join first
    const { data: joinedData, error: joinError } = await supabase
      .from('01_delivery_boys')
      .select(`
        *,
        user:01_users (
          full_name,
          first_name,
          phone,
          email
        )
      `)
      .eq('email', email)
      .maybeSingle();

    if (!joinError && joinedData) {
      deliveryBoyData = joinedData;
      userProfileData = joinedData.user;
    } else {
      // Fallback: Query tables individually and perform the join in application code
      const { data: dbBoy, error: dbBoyErr } = await supabase
        .from('01_delivery_boys')
        .select('*')
        .eq('email', email)
        .maybeSingle();

      if (dbBoy) {
        deliveryBoyData = dbBoy;
        const { data: usrProfile } = await supabase
          .from('01_users')
          .select('*')
          .eq('auth_user_id', authData.user.id)
          .maybeSingle();
        userProfileData = usrProfile;
      }
    }

    if (!deliveryBoyData) {
      // Sign out since user does not have permission
      await supabase.auth.signOut();
      throw new Error('Access Denied: You do not have an active Delivery Boy profile.');
    }

    if (deliveryBoyData.status === 'suspended' || deliveryBoyData.status === 'inactive') {
      await supabase.auth.signOut();
      throw new Error('Access Denied: Your account has been suspended or deactivated.');
    }

    // Determine name from userProfileData first, then deliveryBoyData
    const resolvedName = userProfileData?.full_name || 
                         userProfileData?.first_name || 
                         deliveryBoyData.full_name || 
                         deliveryBoyData.name || 
                         'Delivery Partner';

    const resolvedPhone = userProfileData?.phone || deliveryBoyData.phone || '';

    return {
      session: authData.session,
      profile: {
        delivery_boy_id: deliveryBoyData.delivery_boy_id || deliveryBoyData.delivery_boy_code || 'DB_' + authData.user.id,
        name: resolvedName,
        email: deliveryBoyData.email || email,
        phone: resolvedPhone,
        is_online: !!deliveryBoyData.is_online,
        vehicle_type: deliveryBoyData.vehicle_type || 'Motorcycle',
        vehicle_number: deliveryBoyData.vehicle_number || '',
        status: deliveryBoyData.status || 'active',
      },
    };
  }

  /**
   * Log out from session
   */
  static async logout(): Promise<void> {
    const { error } = await supabase.auth.signOut();
    if (error) {
      throw new Error(error.message);
    }
  }

  /**
   * Get current authenticated user session and corresponding delivery profile via join
   */
  static async getCurrentSession(): Promise<{ session: any; profile: DeliveryBoy | null }> {
    const { data: { session }, error: sessionError } = await supabase.auth.getSession();
    if (sessionError || !session) {
      return { session: null, profile: null };
    }

    const email = session.user?.email || '';
    if (!email) {
      return { session, profile: null };
    }

    let deliveryBoyData: any = null;
    let userProfileData: any = null;

    // Relational join query
    const { data: joinedData, error: joinError } = await supabase
      .from('01_delivery_boys')
      .select(`
        *,
        user:01_users (
          full_name,
          first_name,
          phone,
          email
        )
      `)
      .eq('email', email)
      .maybeSingle();

    if (!joinError && joinedData) {
      deliveryBoyData = joinedData;
      userProfileData = joinedData.user;
    } else {
      // Fallback
      const { data: dbBoy } = await supabase
        .from('01_delivery_boys')
        .select('*')
        .eq('email', email)
        .maybeSingle();

      if (dbBoy) {
        deliveryBoyData = dbBoy;
        const { data: usrProfile } = await supabase
          .from('01_users')
          .select('*')
          .eq('auth_user_id', session.user.id)
          .maybeSingle();
        userProfileData = usrProfile;
      }
    }

    if (!deliveryBoyData) {
      return { session, profile: null };
    }

    const resolvedName = userProfileData?.full_name || 
                         userProfileData?.first_name || 
                         deliveryBoyData.full_name || 
                         deliveryBoyData.name || 
                         'Delivery Partner';

    const resolvedPhone = userProfileData?.phone || deliveryBoyData.phone || '';

    return {
      session,
      profile: {
        delivery_boy_id: deliveryBoyData.delivery_boy_id || deliveryBoyData.delivery_boy_code || 'DB_' + session.user.id,
        name: resolvedName,
        email: deliveryBoyData.email || email,
        phone: resolvedPhone,
        is_online: !!deliveryBoyData.is_online,
        vehicle_type: deliveryBoyData.vehicle_type || 'Motorcycle',
        vehicle_number: deliveryBoyData.vehicle_number || '',
        status: deliveryBoyData.status || 'active',
      },
    };
  }
}
