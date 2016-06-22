package com.foofv.crawler.entity

import com.foofv.crawler.downloader.Response

/**
 * entity that comes from storage(eg. hbase)
 * we parse using it
 * @author soledede
 * @email: wengbenjue@163.com
 */
private[crawler]
class ResObj(val tastEntity: CrawlerTaskEntity,val response: Response) extends Serializable