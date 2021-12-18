# BK-CI

此Chart用于在Kubernetes集群中通过helm部署bkci

## 环境要求
- Kubernetes 1.12+
- Helm 3+

## 安装Chart
使用以下命令安装名称为`bkci`的release, 其中`<bkci helm repo url>`代表helm仓库地址:

```shell
$ helm repo add bkee <bkci helm repo url>
$ helm install bkci bkee/bkrepo
```

上述命令将使用默认配置在Kubernetes集群中部署bkci, 并输出访问指引。

## 卸载Chart
使用以下命令卸载`bkci`:

```shell
$ helm uninstall bkci
```

上述命令将移除所有和bkrepo相关的Kubernetes组件，并删除release。

## Chart依赖
- [bitnami/nginx-ingress-controller](https://github.com/bitnami/charts/tree/master/bitnami/nginx-ingress-controller)
- [bitnami/mysql](https://github.com/bitnami/charts/blob/master/bitnami/mysql)
- [bitnami/redis](https://github.com/bitnami/charts/blob/master/bitnami/redis)
- [bitnami/elasticsearch](https://github.com/bitnami/charts/blob/master/bitnami/elasticsearch)
- [bitnami/rabbitmq](https://github.com/bitnami/charts/blob/master/bitnami/rabbitmq)
- [bitnami/influxdb](https://github.com/bitnami/charts/blob/master/bitnami/influxdb)
- [bitnami/mongodb](https://github.com/bitnami/charts/blob/master/bitnami/mongodb)

## 配置说明
下面展示了可配置的参数列表以及默认值

### RBAC配置

|参数|描述|默认值|
|---|---|---|
| `rbac.serviceAccount` | RBAC账户 | `bkci` |

### 镜像配置

能够配置的镜像有:
- gatewayImage
- backendImage

|参数|描述|默认值|
|---|---|---|
| `registry` | 镜像仓库 | `mirrors.tencent.com/bkce` |
| `repository` | 镜像名称 | `bkci/gateway` / `bkci/backend` |
| `tag` | 镜像tag | `1.16.0` |
| `pullPolicy` | 镜像拉取策略 | `IfNotPresent` |
| `pullSecrets` | 镜像拉取Secret名称数组 | `[]` |

### 蓝鲸日志采集配置
|参数|描述|默认值|
|---|---|---|
| `bklogConfig.enabled` | 是否开启日志采集 | `false` |
| `bklogConfig.service.dataId` | 服务日志采集ID | `1` |
| `bklogConfig.gatewayAccess.dataId` | 网关访问日志采集ID | `1` |
| `bklogConfig.gatewayError.dataId` | 网关异常日志采集ID | `1` |

### 蓝鲸监控配置
|参数|描述|默认值|
|---|---|---|
| `bkmonitorConfig.enabled` | 是否开启蓝鲸监控 | `false` |

### ingress 配置

|参数|描述|默认值 |
|---|---|---|
| `ingress.enabled` | 是否创建ingress | `true` |
| `annotations` | ingress标注 | Check `values.yaml` |

默认不会部署`nginx-ingress-controller`
相关配置请参考[bitnami/nginx-ingress-controller](https://github.com/bitnami/charts/tree/master/bitnami/)

|参数|描述|默认值 |
|---|---|---|
| `nginx-ingress-controller.enabled` | 是否部署nginx ingress controller | `false` |
| `nginx-ingress-controller.defaultBackend.enabled` | nginx ingress controller默认backend | `false` |

### 组件配置

能够配置的组件有:
- artifactory
- auth
- dispatch
- dispatch-docker
- environment
- gateway
- image
- log
- misc
- notify
- openapi
- plugin
- process
- project
- quality
- repository
- store
- ticket
- websocket

|参数|描述|默认值 |
|---|---|---|
| `replicas`                       | Number of pod 1                                                                      | `2`                                                     |
| `resources.limits`                   | The resources limits for containers                                                          | `{cpu:500m ,memory:1500Mi}`                                                    |
| `resources.requests`                 | The requested resources for containers                                                       | `{cpu:100m ,memory:1000Mi}`                                                    |
| `affinity`                           | Affinity for pod assignment (evaluated as a template)                                                                   | `{}`                           |
| `containerSecurityContext.enabled`      | Enable containers' Security Context                                                                             | `false`                                      |
| `containerSecurityContext.runAsUser`    | Containers' Security Context                                                                                    | `1001`                                      |
| `containerSecurityContext.runAsNonRoot` | Containers' Security Context Non Root                                                                           | `true`                                      |
| `nodeAffinityPreset.key`             | Node label key to match Ignored if `affinity` is set.                                                                   | `""`                           |
| `nodeAffinityPreset.type`            | Node affinity preset type. Ignored if `affinity` is set. Allowed values: `soft` or `hard`                               | `""`                           |
| `nodeAffinityPreset.values`          | Node label values to match. Ignored if `affinity` is set.                                                               | `[]`                           |
| `nodeSelector`                       | Node labels for pod assignment                                                                                          | `{}` (evaluated as a template) |
| `podLabels`                             | Add additional labels to the pod (evaluated as a template)                                                            | `nil`                                       |
| `podAnnotations`                     | Pod annotations                                                                                                         | `{}` (evaluated as a template) |
| `podAffinityPreset`                  | Pod affinity preset. Ignored if `affinity` is set. Allowed values: `soft` or `hard`                                     | `""`                           |
| `podAntiAffinityPreset`              | Pod anti-affinity preset. Ignored if `affinity` is set. Allowed values: `soft` or `hard`                                | `soft`                         |
| `podSecurityContext.enabled`         | Enable pod security context                                                                                             | `true`                         |
| `podSecurityContext.fsGroup`         | fsGroup ID for the pod                                                                                                  | `1001`                         |
| `priorityClassName`                     | Define the priority class name for the pod.                                                        | `""`                                        |
| `tolerations`                        | Tolerations for pod assignment                                                                                          | `[]` (evaluated as a template) |

其中除了`gateway` , 其他组件还可以配置jvm的内存

|参数|描述|默认值 |
|---|---|---|
| `env.JVM_XMS` | JVM初始内存 | `512m` |
| `env.JVM_XMX` | JVM最大内存(不能超过limit) | `1024m` | 

### mysql 配置
默认将部署 mysql ，如果不需要可以关闭。
相关配置请参考[bitnami/mysql](https://github.com/bitnami/charts/blob/master/bitnami/mysql)

|参数|描述|默认值 |
|---|---|---|
| `mysql.enabled` | 是否部署mysql。如果需要使用外部数据库，设置为`false`并配置`config.bkCiMysqlxxxx` | `true` |
| `redis.auth.rootPassword` | root密码 | `root` |

### redis 配置
默认将部署 redis , 如果不需要可以关闭。
相关配置请参考[bitnami/redis](https://github.com/bitnami/charts/blob/master/bitnami/redis)

|参数|描述|默认值 |
|---|---|---|
| `redis.enabled` | 是否部署redis。如果需要使用外部数据库，设置为`false`并配置`config.bkCiRedisxxxx` | `true` |
| `redis.auth.password` | 密码 | `user` |

### elasticsearch 配置
默认将部署 elasticsearch , 如果不需要可以关闭。
相关配置请参考[bitnami/elasticsearch](https://github.com/bitnami/charts/blob/master/bitnami/elasticsearch)

|参数|描述|默认值 |
|---|---|---|
| `elasticsearch.enabled` | 是否部署elasticsearch。如果需要使用外部数据库，设置为`false`并配置`config.bkCiEsxxxx` | `true` |

### rabbitmq 配置
默认将部署 rabbitmq , 如果不需要可以关闭。
相关配置请参考[bitnami/rabbitmq](https://github.com/bitnami/charts/blob/master/bitnami/rabbitmq)

|参数|描述|默认值 |
|---|---|---|
| `rabbitmq.enabled` | 是否部署rabbitmq。如果需要使用外部数据库，设置为`false`并配置`config.bkCiRabbitmqxxxx` | `true` |
| `rabbitmq.auth.username` | 用户名 | `user` |
| `rabbitmq.auth.password` | 密码 | `user` |

### influxdb 配置
默认将部署 influxdb , 如果不需要可以关闭。
相关配置请参考[bitnami/influxdb](https://github.com/bitnami/charts/blob/master/bitnami/influxdb)

|参数|描述|默认值 |
|---|---|---|
| `influxdb.enabled` | 是否部署influxdb。如果需要使用外部数据库，设置为`false`并配置`config.bkCiInfluxdbxxxx` | `true` |
| `influxdb.auth.admin.username` | 用户名 | `user` |
| `influxdb.auth.admin.password` | 密码 | `password` |

### mongodb 配置
默认将部署 mongodb , 如果不需要可以关闭。
相关配置请参考[bitnami/mongodb](https://github.com/bitnami/charts/blob/master/bitnami/mongodb)

|参数|描述|默认值 |
|---|---|---|
| `mongodb.enabled` | 是否部署mongodb。如果需要使用外部数据库，设置为`false`并配置`external.mongodb` | `true` |

### 数据持久化配置

数据持久化配置, 当使用filesystem方式存储时需要配置。

|参数|描述|默认值 |
|---|---|---|
| `persistence.enabled` | 是否开启数据持久化，false则使用emptyDir类型volume, pod结束后数据将被清空，无法持久化 | `true` |
| `persistence.accessMode` | PVC Access Mode for bkrepo data volume | `ReadWriteOnce` |
| `persistence.size` | PVC Storage Request for bkrepo data volume | `10Gi` |
| `persistence.storageClass` | 指定storageClass。如果设置为"-", 则禁用动态卷供应; 如果不设置, 将使用默认的storageClass(minikube上是standard) | `nil` |
| `persistence.existingClaim` | 如果开启持久化并且定义了该项，则绑定k8s集群中已存在的pvc | `nil` |
| `persistence.mountPath` | pv挂载的路径 | `/data1` |

> 如果开启数据持久化，并且没有配置`existingClaim`，将使用[动态卷供应](https://kubernetes.io/docs/concepts/storage/dynamic-provisioning/)提供存储，使用`storageClass`定义的存储类。**在删除该声明后，这个卷也会被销毁(用于单节点环境，生产环境不推荐)。**。

### 服务配置
*以下host如果使用k8s的service name , 请使用全限定名称 , 如 bkssm-web.default.svc.cluster.local*

|参数|描述|默认值 |
|---|---|---|
| `bkCiAppCode`  | 应用Code | `"bk_ci"` |
| `bkCiAppToken`  | 应用Token | `""` |
| `bkCiArtifactoryRealm`  | 仓库使用类型 | `local` |
| `bkCiAuthProvider`  | 鉴权方式 | `sample` |
| `bkCiBkrepoAuthorization`  | 制品库鉴权标识 | `""` |
| `bkCiDataDir`  | 数据目录 | `/data/dir/` |
| `bkCiDockerImagePrefix`  | Docker镜像前缀 | `""` |
| `bkCiDockerRegistryPassword`  | Docker仓库密码 | `""` |
| `bkCiDockerRegistryUrl`  | Docker仓库地址 | `""` |
| `bkCiDockerRegistryUser`  | Docker仓库用户 | `""` |
| `bkCiDockerUrl`  | Docker的web入口 | `""` |
| `bkCiDocsUrl`  | 文档地址 | `https://docs.bkci.net/` |
| `bkCiEnvironmentAgentCollectorOn`  | 第三方构建机状态上报 | `true` |
| `bkCiEsClusterName`  | ES的集群名 | `devops` |
| `bkCiEsPassword`  | ES的密码 | `""` |
| `bkCiEsRestAddr`  | ES的地址 | `""` |
| `bkCiEsRestPort`  | ES的端口 | `80` |
| `bkCiEsUser`  | ES的用户名 | `""` |
| `bkCiFqdn`  | CI的其他域名,空格分隔 | `""` |
| `bkCiFqdnCert`  | BKCI站点的HTTPS证书存储位置 | `""` |
| `bkCiGatewayCorsAllowList`  | 网关允许cors的来源域名 | `""` |
| `bkCiGatewayDnsAddr`  | 网关使用的dns | `local=on` |
| `bkCiGatewayRegionName`  | 网关的区域 | `""` |
| `bkCiGatewaySsmTokenUrl`  | 网关用户认证token验证URL的路径 | `""` |
| `bkCiHome`  | CI根目录 | `/data/bkee/ci` |
| `bkCiHost`  | CI域名 | `devops.example.com` |
| `bkCiHttpsPort`  | CI使用https时的端口 | `80` |
| `bkCiHttpPort`  | CI使用http时的端口 | `80` |
| `bkCiIamCallbackUser`  | 供iam系统发起回调时使用的用户名 | `"bk_iam"` |
| `bkCiIamWebUrl`  | IAM SaaS入口url | `""` |
| `bkCiInfluxdbAddr`  | influxdb地址 | `""` |
| `bkCiInfluxdbDb`  | influxdb数据库 | `"agentMetrix"` |
| `bkCiInfluxdbHost`  | influxdb的host | `""` |
| `bkCiInfluxdbPassword`  | influxdb密码 | `""` |
| `bkCiInfluxdbPort`  | influxdb端口 | `80` |
| `bkCiInfluxdbUser`  | influxdb用户 | `""` |
| `bkCiJfrogFqdn`  | jFrog完全合格域名 | `""` |
| `bkCiJfrogHttpPort`  | jFrog构件下载服务的http端口 | `80` |
| `bkCiJfrogPassword`  | jFrog构件下载服务的密码 | `""` |
| `bkCiJfrogUrl`  | jFrog构件下载服务的链接 | `""` |
| `bkCiJfrogUser`  | jFrog构件下载服务的用户 | `""` |
| `bkCiJobFqdn`  | job完全合格域名 | `""` |
| `bkCiJwtRsaPrivateKey`  | JWT RSA密钥对 | `""` |
| `bkCiJwtRsaPublicKey`  | JWT RSA密钥对 | `""` |
| `bkCiLogsDir`  | 日志存放地址 | `/data/logs` |
| `bkCiLogCloseDay`  | 定时清理构建日志--关闭索引 | `""` |
| `bkCiLogDeleteDay`  | 定时清理构建日志--删除索引 | `""` |
| `bkCiLogStorageType`  | 日志存储方式 lucene/elasticsearch | `elasticsearch` |
| `bkCiLuceneDataDir`  | log直接使用lucene时的数据目录 | `""` |
| `bkCiLuceneIndexMaxSize`  | log直接使用lucene时最大值 | `""` |
| `bkCiMysqlAddr`  | mysql地址 | `""` |
| `bkCiMysqlPassword`  | mysql密码 | `""` |
| `bkCiMysqlUser`  | mysql用户 | `""` |
| `bkCiPaasDialogLoginUrl`  | 蓝鲸登录小窗 | `""` |
| `bkCiPaasLoginUrl`  | 跳转到蓝鲸登录服务主页 | `""` |
| `bkCiPrivateUrl`  | 蓝鲸集群内使用的url, 如iam回调ci时 | `""` |
| `bkCiProcessEventConcurrent`  | process并发保护 | `10` |
| `bkCiPublicUrl`  | CI的域名 | `devops.example.com` |
| `bkCiPublicHostIp` | 对外IP | `127.0.0.1` |
| `bkCiRabbitmqAddr`  | rabbitmq地址 | `""` |
| `bkCiRabbitmqPassword`  | rabbitmq密码 | `""` |
| `bkCiRabbitmqUser`  | rabbitmq用户 | `""` |
| `bkCiRabbitmqVhost`  | rabbitmq虚拟地址 | `""` |
| `bkCiRedisDb`  | redis数据库 | `0` |
| `bkCiRedisHost`  | redis地址 | `""` |
| `bkCiRedisPassword`  | redis密码 | `""` |
| `bkCiRedisPort`  | redis端口 | `80` |
| `bkCiRedisSentinelAddr`  | redis哨兵地址 | `""` |
| `bkCiRedisSentinelMasterName`  | redis哨兵名称 | `""` |
| `bkCiRepositoryGithubApp`  | github配置 | `""` |
| `bkCiRepositoryGithubClientId`  | github配置 | `""` |
| `bkCiRepositoryGithubClientSecret`  | github配置 | `""` |
| `bkCiRepositoryGithubSignSecret`  | github配置 | `""` |
| `bkCiRepositoryGitlabUrl`  | gitlab配置 | `""` |
| `bkCiRepositoryGitPluginGroupName`  | git插件分组 | `""` |
| `bkCiRepositoryGitPrivateToken`  | git的token | `""` |
| `bkCiRepositoryGitUrl`  | git地址 | `""` |
| `bkCiRepositorySvnApiKey`  | svn的key | `""` |
| `bkCiRepositorySvnApiUrl`  | svn的地址 | `""` |
| `bkCiRepositorySvnWebhookUrl`  | svn的回调地址 | `""` |
| `bkCiS3AccessKey`  | s3的访问key | `""` |
| `bkCiS3BucketName`  | s3的名称 | `""` |
| `bkCiS3EndpointUrl`  | s3的端点 | `""` |
| `bkCiS3SecretKey`  | s3的秘钥 | `""` |
| `bkCiStoreUserAvatarsUrl`  | PaaS用户头像, 目前仅显示默认头像 | `""` |
| `bkDomain`  | 建议使用用户持有的公网域名(但解析为内网IP) | `""` |
| `bkHome`  | 蓝鲸根目录 | `""` |
| `bkHttpSchema`  | http协议 | `http` |
| `bkIamPrivateUrl`  | iam内部地址 | `""` |
| `bkLicensePrivateUrl`  | 协议内部地址 | `""` |
| `bkPaasFqdn`  | paas域名 | `""` |
| `bkPaasHttpsPort`  | paas端口 | `80` |
| `bkPaasPrivateUrl`  | paas内部地址 | `""` |
| `bkPaasPublicUrl`  | paas外部地址 | `""` |
| `bkRepoHost`  | 制品库地址 | `""` |
| `bkSsmHost`  | 用户认证地址 | `""` |
| `bkSsmPort`  | 用户认证端口 | `80` |
| `bkCiNotifyWeworkSendChannel` | 通知渠道 | `weworkAgent` |

### 编译加速配置
|参数|描述|默认值 |
|---|---|---|
| `turbo.enabled`  | 是否开启编译加速 | `"false"` |
| `turbo.config.tbs.rootpath`  | 编译加速的地址 | `""` |
| `turbo.config.tbs.urltemplate`  | 编译加速的调用url | `"api/v1/{engine}/resource/{resource_type}"` |
| `turbo.config.tbs.dashboard`  | 编译加速管理地址 | `""` |
| `turbo.config.devops.rootpath`  | 蓝盾url | `""` |

### 代码检查配置

**以下为除Kubernetes组件通用配置之外的配置列表**

您可以通过`--set key=value[,key=value]`来指定参数进行安装。例如，

```shell
$ helm install bkci bkee/bkci \
  --set global.imageRegistry=your.registry.com \
  --set gateway.host=your.bkci.com
```


另外，也可以通过指定`YAML`文件的方式来提供参数，

```shell
$ helm install bkci bkee/bkci -f values
```

可以使用`helm show values`来获取默认配置，

```shell
# 查看默认配置
$ helm show values bkee/bkci

# 保存默认配置到文件values.yaml
$ helm show values bkee/bkci > values.yaml
```

