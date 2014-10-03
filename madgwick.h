//=====================================================================================================
// MadgwickAHRS.h
//=====================================================================================================
//
// Implementation of Madgwick's IMU and AHRS algorithms.
// See: http://www.x-io.co.uk/node/8#open_source_ahrs_and_imu_algorithms
//
// Date			Author          Notes
// 29/09/2011	SOH Madgwick    Initial release
// 02/10/2011	SOH Madgwick	Optimised for reduced CPU load
//
//=====================================================================================================

// modified by Jarno Leppänen

#ifndef madgwick_h
#define madgwick_h

//----------------------------------------------------------------------------------------------------
// Variable declaration

//extern volatile float beta;				// algorithm gain
//extern volatile float q0, q1, q2, q3;	// quaternion of sensor frame relative to auxiliary frame

struct orientation {
	float q0;
	float q1;
	float q2;
	float q3;
};

//---------------------------------------------------------------------------------------------------
// Function declarations

void madgwick_init(struct orientation *o);

void madgwick_update_array(struct orientation *o, float sampleFreq, float beta,
		float *values);

void madgwick_update(
		struct orientation *o, float sampleFreq, float beta,
		float gx, float gy, float gz,
		float ax, float ay, float az,
		float mx, float my, float mz);

void madgwick_update_imu(
		struct orientation *o, float sampleFreq, float beta,
		float gx, float gy, float gz,
		float ax, float ay, float az);

#endif
//=====================================================================================================
// End of file
//=====================================================================================================
