import com.nishtahir.MultiVersionTest

def testAndroidVersion = System.getProperty('org.gradle.android.testVersion')

runner {
    if (testAndroidVersion) {
        include MultiVersionTest
    } else {
        exclude MultiVersionTest
    }
}
