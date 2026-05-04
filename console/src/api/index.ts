import { axiosInstance } from "@halo-dev/api-client";
import {
  ConsoleApiPhotoHaloRunV1alpha1PhotoApi,
  ConsoleApiPhotoHaloRunV1alpha1PhotoGroupApi,
  PhotoGroupV1alpha1Api,
  PhotoV1alpha1Api,
} from "./generated";

const photosCoreApiClient = {
  photo: new PhotoV1alpha1Api(undefined, "", axiosInstance),
  group: new PhotoGroupV1alpha1Api(undefined, "", axiosInstance),
};

const photosConsoleApiClient = {
  photo: new ConsoleApiPhotoHaloRunV1alpha1PhotoApi(undefined, "", axiosInstance),
  group: new ConsoleApiPhotoHaloRunV1alpha1PhotoGroupApi(undefined, "", axiosInstance),
};

export { photosConsoleApiClient, photosCoreApiClient };
