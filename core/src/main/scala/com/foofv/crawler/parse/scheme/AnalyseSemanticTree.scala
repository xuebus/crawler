package com.foofv.crawler.parse.scheme

import java.io.{File, InputStream}
import javax.xml.parsers.DocumentBuilderFactory

import com.foofv.crawler.parse.scheme.nodes._
import com.foofv.crawler.util.Logging
import org.w3c.dom.{NodeList, Node, Element, Document}

import scala.collection.mutable
import scala.collection.JavaConversions._

/**
 * Created by soledede on 2015/8/18.
 * Creating semantic tree for scheme
 */
private[crawler]
class AnalyseSemanticTree extends Logging {

  var tree: mutable.Map[String, SemanticNode] = new mutable.HashMap[String, SemanticNode]()
  var dRootNode: SemanticNode = null
  var tableName: String = "sys_soledede"


  def newDocEl(scheme: InputStream): AnalyseSemanticTree = {
    analyse(DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(scheme).getDocumentElement, dRootNode)
    iteratorNode(dRootNode)
    dRootNode = null
    this
  }

  def newDocEl(scheme: File): AnalyseSemanticTree = {
    analyse(DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(scheme).getDocumentElement, dRootNode)
    iteratorNode(dRootNode)
    dRootNode = null
    this
  }

  def newDocEl(scheme: String): AnalyseSemanticTree = {
    analyse(DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(scheme).getDocumentElement, dRootNode)
    iteratorNode(dRootNode)
    dRootNode = null
    this
  }

  def iteratorNode(dRootNode: SemanticNode): Unit = {
    if (dRootNode != null) {
      if (dRootNode.hasChild()) {
        for (ch: SemanticNode <- dRootNode.children) {
          if (ch.isInstanceOf[DocumentNode]) {
            val docNode = ch.asInstanceOf[DocumentNode]
            docNode.extractPost()
            tree(docNode.docId) = docNode
          } else {
            iteratorNode(ch)
          }
        }
      }
    }
  }

  def analyse(docXml: Node, treeNode: SemanticNode): Unit = {
    if (docXml.isInstanceOf[Element]) {
      val xmlEl: Element = docXml.asInstanceOf[Element]
      val tagName: String = xmlEl.getTagName.toLowerCase
      var childNode: SemanticNode = null
      childNode = genereNode(docXml)
      if (childNode != null) {
        if (dRootNode != null) {
          treeNode + childNode
          val xmlChildNodeList: NodeList = docXml.getChildNodes
          var i: Int = 0
          while (i < xmlChildNodeList.getLength) {
            analyse(xmlChildNodeList.item(i), childNode)
            i += 1
          }
        } else {
          dRootNode = new DocumentsNode
          if (childNode.isInstanceOf[DocumentsNode]) analyse(docXml, dRootNode)
        }
      }

    }
  }

  def genereNode(node: Node): SemanticNode = {
    if (node.isInstanceOf[Element]) {
      val element: Element = node.asInstanceOf[Element]
      val tagName: String = element.getTagName.toLowerCase
      if ("docs".equalsIgnoreCase(tagName)) {
        val documents: DocumentsNode = new DocumentsNode
        tableName = documents.getTable(element)
        documents.getRegex(element)
        documents.getGroup(element)
        documents.getFilterGroup(element)
        documents.getFilterRegex(element)
        return documents
      } else if (tagName == "doc") {
        val document: DocumentNode = new DocumentNode
        document.getDocId(element)
        document.getDocUrl(element)
        document.getDocFirstUrl(element)
        document.getIntelligent(element)
        document.getMethod(element)
        document.getPage(element)
        document.getPageSize(element)
        document.getFirstPagesize(element)
        document.getCSSSelector(element)
        document.getName(element)
        document.getJsonSelector(element)
        document.getnotSave(element)
        document.getParameter(element)
        document.getFirstDoc(element)
        document.getTableName(tableName, element)
        document.getRegex(element)
        document.getGroup(element)
        document.getFilterGroup(element)
        document.getFilterRegex(element)
        document.getmaxPage(element)
        document.getSql(element)
        document.getIsStream(element)
        document.getReplace(element)
        document.getStartPage(element)
        return document
      } else if ("list".equalsIgnoreCase(tagName)) {
        val list: ListNode = new ListNode
        list.getCSSSelector(element)
        list.getJsonSelector(element)
        list.getName(element)
        list.getnotSave(element)
        list.getParameter(element)
        list.getIsFiled(element)
        list.getRegex(element)
        list.getGroup(element)
        list.getFilterGroup(element)
        list.getFilterRegex(element)
        list.getfilterNot(element)
        list.getUseContext(element)
        list.getIfExist(element)
        list.getDefault(element)
        list.getReplace(element)
        return list
      } else if ("attribute".equalsIgnoreCase(tagName) || "attr".equalsIgnoreCase(tagName)) {
        val attr: AttributeNode = new AttributeNode
        attr.getCSSSelector(element)
        attr.getJsonSelector(element)
        attr.getName(element)
        attr.getnotSave(element)
        attr.getParameter(element)
        attr.getAttribute(element)
        attr.getIndex(element)
        attr.getRegex(element)
        attr.getGroup(element)
        attr.getFilterGroup(element)
        attr.getFilterRegex(element)
        attr.getUseContext(element)
        attr.getNeedCache128(element)
        attr.getIfExist(element)
        attr.getDefault(element)
        attr.getValue(element)
        attr.getLast(element)
        attr.getReplace(element)
        return attr
      } else if ("str".equalsIgnoreCase(tagName)) {
        val str: StringNode = new StringNode
        str.getCSSSelector(element)
        str.getJsonSelector(element)
        str.getName(element)
        str.getnotSave(element)
        str.getParameter(element)
        str.getValue(element)
        str.getRegex(element)
        str.getGroup(element)
        str.getFilterGroup(element)
        str.getFilterRegex(element)
        str.getNeedCache128(element)
        str.getNeedCache(element)
        str.getIfExist(element)
        str.getDefault(element)
        str.getLast(element)
        str.getReplace(element)
        return str
      } else if ("text".equalsIgnoreCase(tagName)) {
        val text: TextNode = new TextNode
        text.getCSSSelector(element)
        text.getJsonSelector(element)
        text.getName(element)
        text.getnotSave(element)
        text.getParameter(element)
        text.getSeprator(element)
        text.getRegex(element)
        text.getItemType(element)
        text.getGroup(element)
        text.getIndex(element)
        text.getFilterGroup(element)
        text.getFilterRegex(element)
        text.getUseContext(element)
        text.getNeedCache128(element)
        text.getNeedCache(element)
        text.getIfExist(element)
        text.getDefault(element)
        text.getLast(element)
        text.getValue(element)
        text.getReplace(element)
        return text
      } else if ("post".equalsIgnoreCase(tagName)) {
        return new PostNode
      } else if ("ref".equalsIgnoreCase(tagName)) {
        val ref: ReferenceNode = new ReferenceNode
        ref.getDocId(element)
        ref.getRegex(element)
        ref.getGroup(element)
        ref.getFilterGroup(element)
        ref.getFilterRegex(element)
        ref.getIfExist(element)
        ref.getDefault(element)
        return ref
      } else null
    } else return null
  }

}
