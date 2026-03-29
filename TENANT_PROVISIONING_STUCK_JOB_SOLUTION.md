## Mevcutta asılı kalan job’ı hemen kurtarmak için (tek seferlik):

update tenant_provisioning_jobs
set status = 'PENDING',
started_at = null,
worker_node = null,
error_message = 'Manually reset from IN_PROGRESS',
updated_at = now()
where status = 'IN_PROGRESS';
#### Sonra worker bu job’ı tekrar alır.

### Hızlı kontrol:

select id, tenant_id, status, started_at, finished_at, retry_count, error_message
from tenant_provisioning_jobs
order by id desc;

### Eski stuck job’ı temizle:
update tenant_provisioning_jobs
set status = 'FAILED',
finished_at = now(),
error_message = 'Manually failed due stuck IN_PROGRESS',
updated_at = now()
where status = 'IN_PROGRESS';