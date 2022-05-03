# InteropEHRate Terminal Remote-to-Device Emergency (R2DE) protocol's HR exchange library
Reference Implementation of the R2D Emergency protocol specified by the InteropEHRate project.

## Installation Guide
The process of integrating the `t-r2de-e` library is quite straightforward, as it is provided as a `jar`, and is hosted in the project's Nexus repository. 

In case a gradle project is created, the following line needs to be inserted in the dependencies section of the build.gradle file:
```
implementation(group:'eu.interopehrate', name:tr2de, version: '0.1.5')
```

If the development team importing the library, is using Maven instead of Gradle, the same dependency must be expressed with the following Maven syntax:
```
<dependency>
	<groupId>eu.interopehrate</groupId>
	<artifactId>tr2de</artifactId>
	<version>0.1.5</version>
</dependency>
```

## User Guide
Using the `t-r2de-e` library means obtaining an instance of the class `R2DEmergencyI` and then invoking its methods to download and decrypt encrypted health data from the S-EHR Cloud. To obtain an instance of `R2DEmergencyI`, developers must use the class `R2DEmergencyFactory`, as shown in the following example:

``` 
R2DEmergencyI r2demergency = R2DEmergencyFactory.create();
```

The `t-r2de-e` library provides methods for requesting access to a Citizen’s health information stored in the S-EHR Cloud, and downloading such information from the S-EHR Cloud. In addition, the latest version of `t-r2de-e` library, includes additional functionalities that can be utilized by HCPs, such as the ability to upload health data related to the emergency to the citizen’s S-EHR Cloud provider, as well as the ability to download metadata information for a specific health record.

The exact methods provided by the `t-r2de-e` library are listed below: 
* `requestAccess`: HCP working in an Healthcare Institution requests access to a citizen’s data stored in a S-EHR Cloud 
* `get`: Download and decryption (using the R2D Encrypted Communication library [D3.17]) of health data that is already uploaded on the S-EHR Cloud. If the data is not found an error message is received.
* `listBuckets`: returns a list of the buckets that are related to a Citizen.Specification of data encryption mechanisms for mobile and web applications
* `listObjects`: returns a list of objects in a specific bucket.
* `getBundlesInfo`: HCP downloads metadata for a specific health data stored in the S-EHR Cloud.
*`create`: HCP encrypts (using the R2D Encrypted Communication library [D3.17]) and uploads health data related to the emergency to the citizen’s S-EHR Cloud.
